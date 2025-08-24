package net.bteuk.network.eventing.listeners;

import lombok.Setter;
import net.bteuk.network.Network;
import net.bteuk.network.TabManager;
import net.bteuk.network.building_companion.BuildingCompanion;
import net.bteuk.network.gui.Gui;
import net.bteuk.network.lib.dto.TabPlayer;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserConnectRequest;
import net.bteuk.network.lib.dto.UserDisconnect;
import net.bteuk.network.lib.dto.UserRemove;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.TextureUtils;
import net.bteuk.network.utils.Time;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;

import static net.bteuk.network.utils.Constants.LOGGER;
import static net.bteuk.network.utils.Constants.MOTD_CONTENT;
import static net.bteuk.network.utils.Constants.MOTD_ENABLED;
import static net.bteuk.network.utils.Constants.SERVER_NAME;

// This class deals with players joining and leaving the network.
public class Connect implements Listener {

    private final Network instance;

    @Setter
    private boolean blockLeaveEvent;

    public Connect(Network instance) {

        this.instance = instance;

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
    public static void handleUserConnectReply(UserConnectReply reply) {

        Bukkit.getScheduler().runTask(Network.getInstance(), () -> {
            // Find the player associated with the uuid.
            Player player =
                    Network.getInstance().getServer().getOnlinePlayers().stream()
                            .filter(p -> p.getUniqueId().toString().equals(reply.getUuid())).findFirst().orElse(null);

            if (player == null) {
                LOGGER.warning("A UserConnectReply was received but no Player exists with their uuid, maybe they have" +
                        " already left?");
                return;
            }

            LOGGER.info(String.format("User connect reply received from the proxy, creating NetworkUser for %s",
                    player.getName()));
            NetworkUser user = new NetworkUser(player, reply);
            Network.getInstance().addUser(user);

            // Hide this player for all players in focus mode.
            Network.getInstance().getUsers().forEach(serverUser -> {
                if (serverUser.isFocusEnabled()) {
                    serverUser.hidePlayer(player);
                }
            });

            // Sends the message of the day to the player, if applicable
            if (MOTD_ENABLED) {
                MiniMessage miniMessage = MiniMessage.miniMessage();

                //Replaces the player placeholder
                String rawMessage = MOTD_CONTENT.replace("%player%", player.getName());

                Component componentMessage = miniMessage.deserialize(rawMessage);
                player.sendMessage(componentMessage);
            }

            // Send offline messages to the player.
            reply.getMessages().forEach(player::sendMessage);

            // Add the player to the scoreboard.
            Network.getInstance().getTab().onPlayerJoin(player);
            player.playSound(Sound.sound(Key.key("block.note_block.bell"), Sound.Source.PLAYER, 1f, 1f));
        });
    }

    public static void handleUserRemove(UserRemove userRemove) {

        // TODO: Implement users that are no longer on the server but 'offline'.
        // TODO: This will then remove them. Currently this is not implemented.
        LOGGER.info(String.format("User remove event received from the Proxy for %s", userRemove.getUuid()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void joinServerEvent(PlayerJoinEvent e) {

        // Block the default connect message, this will be sent by the proxy.
        e.joinMessage(null);

        // Determine the chat channels to which this user has access.
        Set<String> channels = NetworkUser.getChannels(e.getPlayer());

        // Get the TabPlayer instance for this player.
        TabPlayer tabPlayer = TabManager.createTabPlayerFromPlayer(e.getPlayer());

        // Send a user connect request to the proxy, this will handle the rest.
        // When the proxy has received the request it'll send a response which will then create the user object on
        // the server.
        UserConnectRequest userConnectRequest = new UserConnectRequest(
                SERVER_NAME, e.getPlayer().getUniqueId().toString(), e.getPlayer().getName(),
                TextureUtils.getTexture(e.getPlayer().getPlayerProfile()), channels, tabPlayer,
                e.getPlayer().hasPermission("group.architect"), e.getPlayer().hasPermission("group.reviewer")
        );
        Bukkit.getScheduler().runTaskAsynchronously(Network.getInstance(),
                () -> Network.getInstance().getChat().sendSocketMessage(userConnectRequest));
        LOGGER.info(String.format("%s connected to the server, sent request to proxy to add player as NetworkUser",
                e.getPlayer().getName()));
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
            LOGGER.warning("User " + e.getPlayer().getName() + " was not available on disconnect!");
            UserDisconnect disconnectEvent = new UserDisconnect();
            disconnectEvent.setUuid(e.getPlayer().getUniqueId().toString());
            disconnectEvent.setServer(SERVER_NAME);
            Bukkit.getScheduler().runTaskAsynchronously(Network.getInstance(),
                    () -> Network.getInstance().getChat().sendSocketMessage(disconnectEvent));
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
        Gui.openInventories.remove(playerUUID);

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
        Bukkit.getScheduler().runTaskAsynchronously(Network.getInstance(),
                () -> Network.getInstance().getChat().sendSocketMessage(userDisconnect));
    }
}
