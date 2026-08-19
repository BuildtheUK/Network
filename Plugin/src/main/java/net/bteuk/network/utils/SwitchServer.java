package net.bteuk.network.utils;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkPlayer;
import net.bteuk.network.core.Constants;
import net.bteuk.network.socket.MessageSender;
import org.btuk.network.lib.dto.SwitchServerEvent;
import org.btuk.network.lib.dto.UserDisconnect;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;

import java.util.Optional;

@Log
public class SwitchServer implements ServerAPI {

    private final Network instance;
    private final Constants constants;
    private final MessageSender messageSender;

    public SwitchServer(Network instance, Constants constants, MessageSender messageSender) {
        this.instance = instance;
        this.constants = constants;
        this.messageSender = messageSender;
    }

    /**
     * Handles a player server switch within the Network. If on standalone, skips everything and returns.
     * @param player The player to switch server
     * @param server The server to switch the player to
     */
    public void switchServer(NetworkPlayer player, String server) {

        if (constants.standalone()) {
            return;
        }

        Optional<NetworkUser> user = instance.getNetworkUserByUuid(player.getUuidAsString());

        // If u is null, cancel.
        if (user.isEmpty()) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        // If the server is null, cancel and notify the player.
        if (server == null) {
            player.sendMessage(ChatUtils.error("An error occurred, server does not exist."));
            instance.getLogger().warning("Player attempting to switch to non-existing server.");

            // Remove any join events that the player may have.
            instance.getGlobalSQL().update("DELETE FROM join_events WHERE uuid='" + player.getUuidAsString() +
                    "';");
            return;
        }

        // Check if the server exists and is online.
        if (!instance.getGlobalSQL().hasRow("SELECT name FROM server_data WHERE name='" + server + "';")) {
            player.sendMessage(ChatUtils.error("The server " + server + " does not exist."));

            // Remove any join events that the player may have.
            instance.getGlobalSQL().update("DELETE FROM join_events WHERE uuid='" + player.getUuidAsString() +
                    "';");
            return;
        } else if (instance.getGlobalSQL()
                .hasRow("SELECT online FROM server_data WHERE name='" + server + "' AND online=0;")) {
            player.sendMessage(ChatUtils.error("The server " + server + " is currently offline."));

            // Remove any join events that the player may have.
            instance.getGlobalSQL().update("DELETE FROM join_events WHERE uuid='" + player.getUuidAsString() +
                    "';");
            return;
        }

        // Set switching to true in user.
        user.get().setSwitching(true);

        // Send switch server event to the proxy.
        UserDisconnect userDisconnect = user.get().createDisconnectEvent();
        SwitchServerEvent switchServerEvent = new SwitchServerEvent(player.getUuidAsString(), server, constants.serverName(),
                userDisconnect);
        messageSender.sendSocketMessage(switchServerEvent);
    }

    public static void switchToExternalServer(Player player) {
        player.transfer("btuk.org", 25565);
    }
}
