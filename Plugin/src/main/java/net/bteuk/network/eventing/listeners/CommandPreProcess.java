package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.btuk.network.lib.dto.ServerShutdown;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Collection;

@Log
public class CommandPreProcess implements Listener {

    private final Network instance;
    private final Constants constants;
    private final Afk afk;
    private final Connect connect;
    private final ServerAPI serverAPI;
    private final MessageSender messageSender;

    public CommandPreProcess(Network instance, Constants constants, Afk afk, Connect connect, ServerAPI serverAPI, MessageSender messageSender) {
        this.instance = instance;
        this.constants = constants;
        this.afk = afk;
        this.connect = connect;
        this.serverAPI = serverAPI;
        this.messageSender = messageSender;
        instance.allowShutdown = false;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommandPreProcess(PlayerCommandPreprocessEvent e) {

        // Reset afk status.
        if (!e.getMessage().startsWith("/afk")) {

            // If player is afk, unset it.
            // Reset last logged time.
            NetworkUser user = instance.getUser(e.getPlayer());

            // If u is null, cancel.
            if (user == null) {
                log.severe("User " + e.getPlayer().getName() + " can not be found!");
                e.getPlayer().sendMessage(ChatUtils.error("User can not be found, please relog!"));
                e.setCancelled(true);
                return;
            }

            user.last_movement = Time.currentTime();
            if (user.isAfk()) {
                user.setAfk(false);
                afk.updateAfkStatus(user, false);
            }
        }

        // Replace /region with /network:region
        if (isCommand(e.getMessage(), "/region")) {
            if (constants.regionsEnabled()) {
                e.setMessage(e.getMessage().replace("/region", "/network:region"));
            }
        } else if (isCommand(e.getMessage(), "/tpll")) {
            if (constants.tpllEnabled()) {
                e.setMessage(e.getMessage().replace("/tpll", "/network:tpll"));
            }
        } else if (isCommand(e.getMessage(), "/server")) {
            if (!constants.standalone()) {
                e.setMessage(e.getMessage().replace("/server", "/network:server"));
            }
        } else if (isCommand(e.getMessage(), "/hdb")) {
            // If the skulls plugin exists and is loaded.
            if (constants.skullsEnabled() && Bukkit.getServer().getPluginManager().getPlugin("skulls") != null) {
                e.setMessage(e.getMessage().replace("/hdb", "/skulls"));
            }
        }
    }

    /**
     * Checks whether a command sent is equal to another command.
     *
     * @param message Command message by the sender
     * @param command Command to check for
     * @return true is equals, false otherwise
     */
    private boolean isCommand(String message, String command) {
        return (message.startsWith(command + " ") || message.equalsIgnoreCase(command));
    }

    private boolean isCommand(String message, String... commands) {
        for (String command : commands) {
            if (isCommand(message, command)) {
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void serverCommand(ServerCommandEvent s) {
        if (s.getCommand().equalsIgnoreCase("stop")) {
            if (!instance.allowShutdown) {
                instance.allowShutdown = true;
                onServerClose(instance.getUsers());

                // Delay shutdown by 3 seconds to make sure players have switched server.
                s.setCancelled(true);
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
                    // Disable the LeaveServer event, although everyone should already be disconnected by now.
                    if (connect != null) {
                        connect.setBlockLeaveEvent(true);
                    }

                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
                }, 60L);
            }
        }
    }

    // This class executes when the server closes, instead of a player server quit event since that will cause errors.
    // For the most part, it copies the methods.
    public void onServerClose(Collection<NetworkUser> users) {

        // Check if another server is online,
        // If true then switch all the players to this server.
        // Always check the lobby and earth first.
        // Remove all players from network.
        String server = null;

        // Try different servers.
        if (!constants.standalone()) {
            if (instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE type='LOBBY' AND online=1 AND name<>'" + constants.serverName() + "';")) {

                server = instance.getGlobalSQL().getString("SELECT name FROM server_data WHERE type='LOBBY' AND online=1 AND name<>'" + constants.serverName() + "';");
            } else if (instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE type='EARTH' AND online=1 AND name<>'" + constants.serverName() + "';")) {

                server = instance.getGlobalSQL().getString("SELECT name FROM server_data WHERE type='EARTH' AND online=1 AND name<>'" + constants.serverName() + "';");
            } else if (instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE online=1 AND name<>'" + constants.serverName() + "';")) {

                server = instance.getGlobalSQL().getString("SELECT name FROM server_data WHERE online=1 AND name<>'" + constants.serverName() + "';");
            }
        }

        for (NetworkUser user : users) {
            // Switch the player to another server, if available, else kick them.
            if (server != null) {
                serverAPI.switchServer(PlayerAdapter.adapt(user.player), server);
            } else {
                // Kick the player.
                instance.getServer().getScheduler().runTask(instance, () -> user.player.kick(Component.text("The server is restarting!", NamedTextColor.RED)));
            }
        }

        // Block movement and teleport listeners.
        instance.moveListener.block();
        instance.teleportListener.block();

        // Remove users from the list.
        users.clear();

        // Let the Proxy know the server is closing.
        messageSender.sendSocketMessage(new ServerShutdown(constants.serverName()));
    }
}
