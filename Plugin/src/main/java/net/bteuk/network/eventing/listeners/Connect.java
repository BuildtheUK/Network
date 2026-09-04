package net.bteuk.network.eventing.listeners;

import lombok.Setter;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.TabManager;
import net.bteuk.network.building_companion.BuildingCompanion;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionUser;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.TextureUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.btuk.minecraft.gui.GuiManager;
import org.btuk.network.lib.dto.TabPlayer;
import org.btuk.network.lib.dto.UserConnectReply;
import org.btuk.network.lib.dto.UserConnectRequest;
import org.btuk.network.lib.dto.UserDisconnect;
import org.btuk.network.lib.dto.UserRemove;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

// This class deals with players joining and leaving the network.
@Log
public class Connect implements Listener {

    private final Network instance;
    private final Constants constants;
    private final TabManager tabManager;
    private final Roles roles;
    private final GuiManager guiManager;
    private final Nightvision nightvision;
    private final EventManager eventManager;
    private final RegionManager regionManager;
    private final MessageSender messageSender;

    @Setter
    private boolean blockLeaveEvent;

    public Connect(Network instance, Constants constants, TabManager tabManager, Roles roles, GuiManager guiManager, Nightvision nightvision,
                   EventManager eventManager, RegionManager regionManager, MessageSender messageSender) {

        this.instance = instance;
        this.constants = constants;
        this.tabManager = tabManager;
        this.roles = roles;
        this.guiManager = guiManager;
        this.nightvision = nightvision;
        this.eventManager = eventManager;
        this.regionManager = regionManager;
        this.messageSender = messageSender;

        this.blockLeaveEvent = false;

        // Register join and leave events.
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    /**
     * When a user connects a request is sent to the proxy.
     * If successful the server receives this reply object.
     * Using the object a {@link NetworkUser} instance is created.
     *
     * @param reply the {@link UserConnectReply}
     */
    public void handleUserConnectReply(UserConnectReply reply) {

        Bukkit.getScheduler().runTask(instance, () -> {
            // Find the player associated with the uuid.
            Player player = instance.getServer().getOnlinePlayers().stream().filter(p -> p.getUniqueId().toString().equals(reply.getUuid())).findFirst().orElse(null);

            if (player == null) {
                log.warning("A UserConnectReply was received but no Player exists with their uuid, maybe they have" + " already left?");
                return;
            }

            log.info(String.format("User connect reply received from the proxy, creating NetworkUser for %s", player.getName()));
            RegionUser regionUser = null;
            if (constants.regionsEnabled()) {
                regionUser = regionManager.getUserByPlayer(player).orElse(null);
            }
            NetworkUser user = new NetworkUser(player, reply, instance, constants, roles, nightvision, regionUser, messageSender);
            instance.addUser(user);

            // Hide this player for all players in focus mode.
            instance.getUsers().forEach(serverUser -> {
                if (serverUser.isFocusEnabled()) {
                    serverUser.hidePlayer(player);
                }
            });

            // Sends the message of the day to the player, if applicable
            if (constants.motdEnabled()) {
                MiniMessage miniMessage = MiniMessage.miniMessage();

                // Replaces the player placeholder
                String rawMessage = constants.motdContent().replace("%player%", player.getName());

                Component componentMessage = miniMessage.deserialize(rawMessage);
                player.sendMessage(componentMessage);
            }

            // Run all their join events.
            user.runEvents(eventManager);

            // Send offline messages to the player.
            reply.getMessages().forEach(player::sendMessage);

            // Add the player to the scoreboard.
            tabManager.onPlayerJoin(player);
            player.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.PLAYER, 1f, 1f));
        });
    }

    public void handleUserRemove(UserRemove userRemove) {

        // TODO: Implement users that are no longer on the server but 'offline'.
        // TODO: This will then remove them. Currently this is not implemented.
        log.info(String.format("User remove event received from the Proxy for %s", userRemove.getUuid()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void joinServerEvent(PlayerJoinEvent joinEvent) {
        networkJoinEvent(joinEvent);
    }

    @EventHandler
    public void leaveServerEvent(PlayerQuitEvent e) {
        e.quitMessage(null);

        if (blockLeaveEvent) {
            return;
        }

        NetworkUser user = instance.getUser(e.getPlayer());

        // If u is null, cancel.
        if (user == null) {
            log.warning("User " + e.getPlayer().getName() + " was not available on disconnect!");
            UserDisconnect disconnectEvent = new UserDisconnect();
            disconnectEvent.setUuid(e.getPlayer().getUniqueId().toString());
            disconnectEvent.setServer(constants.serverName());
            Bukkit.getScheduler().runTaskAsynchronously(instance, () -> messageSender.sendSocketMessage(disconnectEvent));
            return;
        }

        // Reset last logged time.
        if (user.isAfk()) {
            user.last_movement = Time.currentTime();
            user.setAfk(false);
        }

        // If the companion is enabled, disable it.
        BuildingCompanion companion = user.getCompanion();
        if (companion != null) {
            companion.disable();
        }

        // Remove user from list.
        instance.removeUser(user);

        // Get player uuid.
        UUID playerUUID = user.player.getUniqueId();

        // If they are currently in an inventory, remove them from the list of open inventories.
        guiManager.closeGui(playerUUID);

        // Delete any guis that may exist.
        if (user.mainGui != null) {
            user.mainGui.delete();
        }
        if (user.staffGui != null) {
            user.staffGui.delete();
        }
        if (user.lightsOut != null) {
            user.lightsOut.delete();
        }

        // Send a disconnect event to the proxy to handle potential messages.
        UserDisconnect userDisconnect = user.createDisconnectEvent();
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> messageSender.sendSocketMessage(userDisconnect));
    }

    private void networkJoinEvent(PlayerJoinEvent e) {
        // Block the default connect message, this will be sent by the proxy.
        e.joinMessage(null);

        // Ensure the player is hidden from the tab list; the proxy will handle adding the player back in the correct way.
        tabManager.hidePlayersFromTabList(e.getPlayer());
        tabManager.hidePlayerInTabList(e.getPlayer());

        // Determine the chat channels to which this user has access.
        Set<String> channels = NetworkUser.getChannels(e.getPlayer());

        // Get the TabPlayer instance for this player.
        TabPlayer tabPlayer = tabManager.createTabPlayerFromPlayer(e.getPlayer());

        // Send a user connect request to the proxy, this will handle the rest.
        // When the proxy has received the request it'll send a response which will then create the user object on
        // the server.
        UserConnectRequest userConnectRequest = new UserConnectRequest(constants.serverName(), e.getPlayer().getUniqueId().toString(), e.getPlayer().getName(),
                TextureUtils.getTexture(e.getPlayer().getPlayerProfile()), channels, tabPlayer, e.getPlayer().hasPermission("group.architect"),
                e.getPlayer().hasPermission("group.reviewer"));
        Bukkit.getScheduler().runTaskAsynchronously(instance, () -> messageSender.sendSocketMessage(userConnectRequest));
        log.info(String.format("%s connected to the server, sent request to proxy to add player as NetworkUser", e.getPlayer().getName()));
    }
}
