package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Spawn extends AbstractCommand {

    private final Constants constants;
    private final Back back;
    private final Lobby lobby;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final SQLAPI globalSQL;
    private final PreviousLocationTracker previousLocationTracker;

    public Spawn(Constants constants, Back back, Lobby lobby, EventAPI eventAPI, ServerAPI serverAPI, SQLAPI globalSQL, PreviousLocationTracker previousLocationTracker) {
        this.constants = constants;
        this.back = back;
        this.lobby = lobby;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
        this.globalSQL = globalSQL;
        this.previousLocationTracker = previousLocationTracker;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check permission.
        if (!hasPermission(player, "uknet.navigation.spawn")) {
            return;
        }

        // If server is Lobby, teleport to spawn.
        NetworkLocation location = LocationAdapter.adapt(player.getLocation());
        if (constants.serverType() == ServerType.LOBBY) {

            previousLocationTracker.setPreviousCoordinate(player.getUniqueId().toString(), location);
            player.teleport(lobby.getSpawn());
            player.sendMessage(ChatUtils.success("Teleported to spawn."));
        } else {

            // Set teleport event to go to spawn.
            eventAPI.createTeleportEvent(true, player.getUniqueId().toString(), "teleport spawn", location);
            serverAPI.switchServer(PlayerAdapter.adapt(player), globalSQL.getString("SELECT name FROM " +
                    "server_data WHERE type='LOBBY';"));
        }
    }

    @Override
    public String getLabel() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Teleport to spawnpoint in lobby.";
    }
}
