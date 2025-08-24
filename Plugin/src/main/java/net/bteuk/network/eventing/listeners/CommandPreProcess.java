package net.bteuk.network.eventing.listeners;

import net.bteuk.network.Network;
import net.bteuk.network.lib.dto.ServerShutdown;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.ArrayList;

import static net.bteuk.network.commands.Afk.updateAfkStatus;
import static net.bteuk.network.utils.Constants.LOGGER;
import static net.bteuk.network.utils.Constants.REGIONS_ENABLED;
import static net.bteuk.network.utils.Constants.SERVER_NAME;
import static net.bteuk.network.utils.Constants.TPLL_ENABLED;

public class CommandPreProcess implements Listener {

    private final Network instance;

    public CommandPreProcess(Network instance) {
        this.instance = instance;
        instance.allow_shutdown = false;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent e) {

        // Reset afk status.
        if (!e.getMessage().startsWith("/afk")) {

            // If player is afk, unset it.
            // Reset last logged time.
            NetworkUser user = instance.getUser(e.getPlayer());

            // If u is null, cancel.
            if (user == null) {
                LOGGER.severe("User " + e.getPlayer().getName() + " can not be found!");
                e.getPlayer().sendMessage(ChatUtils.error("User can not be found, please relog!"));
                e.setCancelled(true);
                return;
            }

            user.last_movement = Time.currentTime();
            if (user.isAfk()) {
                user.setAfk(false);
                updateAfkStatus(user, false);
            }
        }

        // Replace /region with /network:region
        if (isCommand(e.getMessage(), "/region")) {
            if (REGIONS_ENABLED) {
                e.setMessage(e.getMessage().replace("/region", "/network:region"));
            }
        } else if (isCommand(e.getMessage(), "/tpll")) {
            if (TPLL_ENABLED) {
                e.setMessage(e.getMessage().replace("/tpll", "/network:tpll"));
            }
        } else if (isCommand(e.getMessage(), "/server")) {
            e.setMessage(e.getMessage().replace("/server", "/network:server"));
        } else if (isCommand(e.getMessage(), "/hdb")) {
            // If skulls plugin exists and is loaded.
            if (Bukkit.getServer().getPluginManager().getPlugin("skulls") != null) {
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
            if (!instance.allow_shutdown) {
                instance.allow_shutdown = true;
                onServerClose(instance.getUsers());

                // Delay shutdown by 3 seconds to make sure players have switched server.
                s.setCancelled(true);
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
                    // Disable the LeaveServer event, although everyone should already be disconnected by now.
                    if (instance.getConnect() != null) {
                        instance.getConnect().setBlockLeaveEvent(true);
                    }

                    Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
                }, 60L);
            }
        }
    }

    // This class executes when the server closes, instead of a player server quit event since that will cause errors.
    // It for the most part copies the methods.
    public void onServerClose(ArrayList<NetworkUser> users) {

        // Check if another server is online,
        // If true then switch all the players to this server.
        // Always check the lobby and earth first.
        // Remove all players from network.
        String server = null;

        // Try different servers.
        if (instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE type='LOBBY' AND online=1 AND name<>'" + SERVER_NAME + "';")) {

            server = instance.getGlobalSQL().getString("SELECT name FROM server_data WHERE type='LOBBY' AND online=1 AND name<>'" + SERVER_NAME + "';");
        } else if (instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE type='EARTH' AND online=1 AND name<>'" + SERVER_NAME + "';")) {

            server = instance.getGlobalSQL().getString("SELECT name FROM server_data WHERE type='EARTH' AND online=1 AND name<>'" + SERVER_NAME + "';");
        } else if (instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE online=1 AND name<>'" + SERVER_NAME + "';")) {

            server = instance.getGlobalSQL().getString("SELECT name FROM server_data WHERE online=1 AND name<>'" + SERVER_NAME + "';");
        }

        for (NetworkUser user : users) {
            // Switch the player to another server, if available, else kick them.
            if (server != null) {
                SwitchServer.switchServer(user.player, server);
            } else {
                // Kick the player.
                instance.getServer().getScheduler().runTask(instance, () -> user.player.kick(Component.text("The server is restarting!", NamedTextColor.RED)));
            }
        }

        // Block movement and teleport listeners.
        instance.moveListener.block();
        instance.teleportListener.block();

        // Remove users from list.
        users.clear();

        // Let the Proxy know the server is closing.
        instance.getChat().sendSocketMessage(new ServerShutdown(SERVER_NAME));
    }
}
