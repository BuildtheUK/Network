package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.LocationSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.GlobalSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Warp extends AbstractCommand {

    private final Constants constants;
    private final PlotAPI plotAPI;
    private final Back back;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final GlobalSQL globalSQL;

    public Warp(Network instance, Constants constants, PlotAPI plotAPI, Back back, EventAPI eventAPI, ServerAPI serverAPI) {
        this.constants = constants;
        this.plotAPI = plotAPI;
        this.back = back;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
        this.globalSQL = instance.getGlobalSQL();
        setTabCompleter(new LocationSelector(globalSQL));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        if (args.length == 0) {
            help(player);
            return;
        }

        // Get the location name from all remaining args.
        String location = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

        // Find a location.
        if (globalSQL.hasRow("SELECT location FROM location_data WHERE location='" + location + "';")) {

            // Get coordinate id.
            int coordinate_id = globalSQL.getInt("SELECT coordinate FROM location_data WHERE location='" + location + "';");

            // Get server, if server is not the current server,
            // teleport the player to the correct server with join event to teleport them to the location.
            String server = globalSQL.getString("SELECT server FROM coordinates WHERE id=" + coordinate_id + ";");
            NetworkLocation currentLocation = LocationAdapter.adapt(player.getLocation());
            if (server.equals(constants.serverName())) {

                // Get location from coordinate id.
                Location l = globalSQL.getLocation(coordinate_id);

                String worldName = globalSQL.getString("SELECT world FROM coordinates WHERE id=" + coordinate_id + ";");

                // Check if world is in plotsystem.
                if (plotAPI.hasLocation(worldName)) {

                    // Add coordinate transformation.
                    l = new Location(
                            Bukkit.getWorld(worldName),
                            l.getX() + plotAPI.getXTransform(worldName),
                            l.getY(),
                            l.getZ() + plotAPI.getZTransform(worldName),
                            l.getYaw(),
                            l.getPitch()
                    );
                }

                // Set current location for /back
                back.setPreviousCoordinate(player.getUniqueId().toString(), currentLocation);

                // Teleport to location.
                player.teleport(l);
                player.sendMessage(ChatUtils.success("Teleported to ")
                        .append(Component.text(location, NamedTextColor.DARK_AQUA)));
            } else {
                eventAPI.createTeleportEvent(true, player.getUniqueId().toString(), "network",
                        "teleport location " + location, currentLocation);
                serverAPI.switchServer(PlayerAdapter.adapt(player), server);
            }
        } else {
            player.sendMessage(ChatUtils.error("The location ")
                    .append(Component.text(location, NamedTextColor.DARK_RED))
                    .append(ChatUtils.error(" does not exist.")));
        }
    }

    private void help(Player p) {
        p.sendMessage(ChatUtils.error("/warp <location>"));
    }

    @Override
    public String getLabel() {
        return "warp";
    }

    @Override
    public String getDescription() {
        return "Warp to locations in the exploration menu.";
    }
}
