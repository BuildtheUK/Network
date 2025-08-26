package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.GlobalSQL;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Back extends AbstractCommand {
    private final Constants constants;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final GlobalSQL globalSQL;

    public Back(Network instance, Constants constants, EventAPI eventAPI, ServerAPI serverAPI) {
        this.constants = constants;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
        this.globalSQL = instance.getGlobalSQL();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Get the coordinate ID.
        int coordinateID = globalSQL.getInt("SELECT previous_coordinate FROM player_data " + "WHERE uuid='" + player.getUniqueId() + "';");

        // Check if the player has a previous coordinate.
        if (coordinateID == 0) {

            player.sendMessage(ChatUtils.error("You have not teleported anywhere previously."));
            return;
        }

        // Check if the server is this server.
        String server = globalSQL.getString("SELECT server FROM coordinates WHERE id=" + coordinateID + ";");
        if (Objects.equals(constants.serverName(), server)) {

            // Get location.
            Location l = globalSQL.getLocation(coordinateID);

            // Set current location to previous location.
            setPreviousCoordinate(player.getUniqueId().toString(), LocationAdapter.adapt(player.getLocation()));

            // Teleport player to the coordinate.
            player.teleport(l);
            player.sendMessage(ChatUtils.success("Teleported to previous location."));
        } else if (!constants.standalone()) {

            // Teleport the player to the correct server with a join event to teleport to the coordinate id.
            // Create teleport event for location of coordinate id
            eventAPI.createTeleportEvent(true, player.getUniqueId().toString(), "network",
                    "teleport " + globalSQL.getString("SELECT world FROM coordinates WHERE id=" + coordinateID + ";") + " " + globalSQL.getDouble(
                            "SELECT x FROM coordinates WHERE id=" + coordinateID + ";") + " " + globalSQL.getDouble(
                            "SELECT y FROM coordinates WHERE id=" + coordinateID + ";") + " " + globalSQL.getDouble(
                            "SELECT z FROM coordinates WHERE id=" + coordinateID + ";") + " " + globalSQL.getFloat(
                            "SELECT yaw FROM coordinates WHERE id=" + coordinateID + ";") + " " + globalSQL.getFloat(
                            "SELECT pitch FROM coordinates WHERE id=" + coordinateID + ";"), "&aTeleport to previous location.", LocationAdapter.adapt(player.getLocation()));

            // Switch server.
            serverAPI.switchServer(PlayerAdapter.adapt(player), server);
        }
    }

    // Sets the location as the previous location in the database.
    public void setPreviousCoordinate(String uuid, NetworkLocation location) {

        // Set previous location for /back.
        if (globalSQL.getInt("SELECT previous_coordinate FROM player_data WHERE uuid='" + uuid + "';") == 0) {

            // No coordinate exists, create new.
            int coordinateID = globalSQL.addCoordinate(location);

            // Set coordinate id in player data.
            globalSQL.update("UPDATE player_data SET previous_coordinate=" + coordinateID + " WHERE uuid='" + uuid + "';");
        } else {

            // Get coordinate id.
            int coordinateID = globalSQL.getInt("SELECT previous_coordinate FROM " + "player_data WHERE uuid='" + uuid + "';");

            // Update existing coordinate.
            globalSQL.updateCoordinate(coordinateID, location);
        }
    }

    @Override
    public String getLabel() {
        return "back";
    }

    @Override
    public String getDescription() {
        return "Teleports the player to the previous teleported location.";
    }
}

