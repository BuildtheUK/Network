package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.PlayerSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.dto.OnlineUser;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class Teleport extends AbstractCommand {

    private final Network instance;
    private final Back back;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final SQLAPI globalSQL;
    private final Constants constants;

    public Teleport(Network instance, Back back, EventAPI eventAPI, ServerAPI serverAPI, Constants constants) {
        this.instance = instance;
        this.back = back;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
        this.globalSQL = instance.getGlobalSQL();
        this.constants = constants;
        setTabCompleter(new PlayerSelector());
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

        // Try and find the player by name.
        Optional<OnlineUser> optionalOnlineUser = instance.getOnlineUserByNameIgnoreCase(args[0]);
        if (optionalOnlineUser.isPresent()) {

            OnlineUser onlineUser = optionalOnlineUser.get();

            // Check if the player has teleport enabled/disabled.
            // If disabled, cancel teleport.
            if (player.hasPermission("uknet.navigation.teleport.bypass") || globalSQL.hasRow(
                    "SELECT uuid FROM player_data WHERE uuid='" + onlineUser.getUuid() + "' AND teleport_enabled=1;")) {

                // If the player is on your server teleport.
                // Else switch server and add teleport join event.
                Optional<NetworkUser> optionalNetworkUser =
                        instance.getNetworkUserByUuid(onlineUser.getUuid());

                NetworkLocation currentLocation = LocationAdapter.adapt(player.getLocation());
                optionalNetworkUser.ifPresentOrElse((NetworkUser user) -> {
                    // Set the current location for /back
                    back.setPreviousCoordinate(player.getUniqueId().toString(), currentLocation);

                    player.teleport(user.player.getLocation());
                    player.sendMessage(ChatUtils.success("Teleported to %s", onlineUser.getName()));
                }, () -> {
                    if (!constants.standalone()) {
                        eventAPI.createTeleportEvent(true, player.getUniqueId().toString(), "network", "teleport " +
                                "player " + onlineUser.getUuid(), currentLocation);
                        serverAPI.switchServer(PlayerAdapter.adapt(player), onlineUser.getServer());
                    }
                });
            } else {
                player.sendMessage(ChatUtils.error("%s has teleport disabled.", onlineUser.getName()));
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
}