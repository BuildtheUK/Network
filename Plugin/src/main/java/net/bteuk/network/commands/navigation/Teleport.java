package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.PlayerSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.dto.OnlineUser;
import net.bteuk.network.lib.dto.TeleportEvent;
import net.bteuk.network.lib.enums.TeleportRequestType;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Log
public class Teleport extends AbstractCommand {

    private final Network instance;
    private final PreviousLocationTracker previousLocationTracker;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final SQLAPI globalSQL;
    private final Constants constants;
    private final MessageSender messageSender;

    public Teleport(Network instance, PreviousLocationTracker previousLocationTracker, EventAPI eventAPI, ServerAPI serverAPI, Constants constants, MessageSender messageSender) {
        this.instance = instance;
        this.previousLocationTracker = previousLocationTracker;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
        this.globalSQL = instance.getGlobalSQL();
        this.constants = constants;
        this.messageSender = messageSender;
        setTabCompleter(new PlayerSelector(instance));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check if args exist.
        if (args.length == 0) {
            player.sendMessage(ChatUtils.error("You must specify a player to teleport to."));
            return;
        }

        // Prevent teleporting to yourself as it could have weird behaviour when tptoggle is enabled.
        if (player.getName().equalsIgnoreCase(args[0])) {
            player.sendMessage(ChatUtils.error("You cannot teleport to yourself."));
            return;
        }

        // Try and find the player by name.
        Optional<OnlineUser> optionalOnlineUser = instance.getOnlineUserByNameIgnoreCase(args[0]);
        if (optionalOnlineUser.isPresent()) {

            OnlineUser onlineUser = optionalOnlineUser.get();

            // Check if the player has teleport enabled/disabled.
            // If disabled, cancel teleport.
            if (player.hasPermission("uknet.navigation.teleport.bypass") || globalSQL.hasRow(
                    "SELECT uuid FROM player_data WHERE uuid='" + onlineUser.getUuid() + "' AND teleport_enabled=1;")) {
                teleport(player, onlineUser);
            } else {
                TeleportEvent teleportEvent = new TeleportEvent(player.getUniqueId().toString(), onlineUser.getUuid(), TeleportRequestType.REQUEST);
                messageSender.sendSocketMessage(teleportEvent);
            }
        } else {
            player.sendMessage(ChatUtils.error("%s is not online.", args[0]));
        }
    }

    @Override
    public String getLabel() {
        return "teleport";
    }

    @Override
    public String getDescription() {
        return "Teleport to any online player.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("tp");
    }

    public void handleTeleportEvent(TeleportEvent teleportEvent) {
        Bukkit.getScheduler().runTask(instance, () -> handleTeleportEventSync(teleportEvent));
    }

    private void handleTeleportEventSync(TeleportEvent teleportEvent) {
        if (teleportEvent.getType() == TeleportRequestType.ACCEPT) {
            Optional<NetworkUser> optionalUser = instance.getNetworkUserByUuid(teleportEvent.getRequester());
            Optional<OnlineUser> target = instance.getOnlineUserByUuid(teleportEvent.getTarget());
            if (optionalUser.isPresent() && target.isPresent()) {
                Player player = optionalUser.get().player;
                player.sendMessage(ChatUtils.success("Teleport request from %s has been accepted, teleporting...", target.get().getName()));
                teleport(player, target.get());
            } else if (optionalUser.isEmpty()) {
                log.severe("User " + teleportEvent.getRequester() + " is not on this server, teleport accept failed.");
            } else {
                log.severe("User " + teleportEvent.getTarget() + " is not online, teleport accept failed.");
            }
        } else {
            log.warning("An invalid teleport event was received: " + teleportEvent.getType());
        }
    }

    private void teleport(Player player, OnlineUser target) {
        // If the player is on your server teleport.
        // Else switch server and add teleport join event.
        Optional<NetworkUser> optionalNetworkUser = instance.getNetworkUserByUuid(target.getUuid());

        NetworkLocation currentLocation = LocationAdapter.adapt(player.getLocation());
        optionalNetworkUser.ifPresentOrElse((NetworkUser user) -> {

            // Check that the player is still online, when switching server, the player could temporarily not be available.
            if (!user.player.isConnected()) {
                player.sendMessage(ChatUtils.error("%s is currently not available, they may have disconnected.", target.getName()));
                return;
            }

            // Set the current location for /back
            previousLocationTracker.setPreviousCoordinate(player.getUniqueId().toString(), currentLocation);

            player.teleport(user.player.getLocation());
            player.sendMessage(ChatUtils.success("Teleported to %s", target.getName()));
        }, () -> {
            if (!constants.standalone()) {
                eventAPI.createTeleportEvent(true, player.getUniqueId().toString(), "teleport " + "player " + target.getUuid(), currentLocation);
                serverAPI.switchServer(PlayerAdapter.adapt(player), target.getServer());
            }
        });
    }
}