package net.bteuk.network.eventing.events;

import io.papermc.lib.PaperLib;
import lombok.extern.java.Log;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.Event;
import net.bteuk.network.commands.navigation.Tpll;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.papercore.WorldUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.sql.GlobalSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.UUID;

@Log
public class TeleportEvent implements Event {

    private final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("##.#####");

    private final GlobalSQL globalSQL;
    private final PlotAPI plotAPI;
    private final RegionManager regionManager;
    private final Constants constants;
    private final ServerAPI serverAPI;
    private final EventAPI eventAPI;
    private final Tpll tpll;
    private final Lobby lobby;

    public TeleportEvent(GlobalSQL globalSQL, PlotAPI plotAPI, RegionManager regionManager, Constants constants, ServerAPI serverAPI, EventAPI eventAPI, Tpll tpll, Lobby lobby) {
        this.globalSQL = globalSQL;
        this.plotAPI = plotAPI;
        this.regionManager = regionManager;
        this.constants = constants;
        this.serverAPI = serverAPI;
        this.eventAPI = eventAPI;
        this.tpll = tpll;
        this.lobby = lobby;
    }

    @Override
    public void event(String uuid, String[] event, String message) {

        // Get player.
        Player p = Bukkit.getPlayer(UUID.fromString(uuid));

        if (p == null) {
            log.warning("Player is null in teleport event.");
            return;
        }

        // Check if the teleport is to a specific player.
        switch (event[1]) {
            case "player" -> {

                // Get player if they're online and teleport the player there.
                Player player = Bukkit.getPlayer(UUID.fromString(event[2]));
                if (player != null) {

                    // Check that the player is still online, when switching server, the player could temporarily not be available.
                    if (!player.isConnected()) {
                        player.sendMessage(ChatUtils.error("%s is currently not available, they may have disconnected.", player.getName()));
                        return;
                    }

                    p.teleport(player.getLocation());
                    p.sendMessage(ChatUtils.success("Teleported to ")
                            .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + event[2] + "';"), NamedTextColor.DARK_AQUA)));
                } else {
                    p.sendMessage(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + event[2] + "';"), NamedTextColor.DARK_RED)
                            .append(ChatUtils.error(" is not online.")));
                }
            }

            // Check if the teleport is to a specific coordinate ID.
            case "coordinateID" -> {

                p.teleport(globalSQL.getLocation(Integer.parseInt(event[2])));

                // Check if a message is set.
                if (message == null) {
                    p.sendMessage(ChatUtils.success("Teleported to previous location."));
                } else {
                    p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
                }
            }
            case "location", "location_request" -> {

                // Get location name from all remaining args.
                String location = String.join(" ", Arrays.copyOfRange(event, 2, event.length));

                // Get the coordinate id.
                int coordinate_id;
                if (event[1].equals("location")) {
                    coordinate_id = globalSQL.getInt("SELECT coordinate FROM location_data" + " WHERE location='" + location + "';");
                } else {
                    coordinate_id = globalSQL.getInt("SELECT coordinate FROM " + "location_requests WHERE location='" + location + "';");
                }

                Location l = globalSQL.getLocation(coordinate_id);

                String worldName = globalSQL.getString("SELECT world FROM coordinates " + "WHERE id=" + coordinate_id + ";");

                // Check if world is in plotsystem.
                if (plotAPI.hasLocation(worldName)) {

                    // Add coordinate transformation.
                    l = new Location(WorldUtils.getWorld(worldName), l.getX() + plotAPI.getXTransform(worldName), l.getY(), l.getZ() + plotAPI.getZTransform(worldName), l.getYaw(),
                            l.getPitch());
                }

                p.teleport(l);
                p.sendMessage(ChatUtils.success("Teleported to ").append(Component.text(location, NamedTextColor.DARK_AQUA)));
            }
            case "region" -> {

                // Get the region.
                Region region = regionManager.getRegion(event[2]);
                int coordinateId = regionManager.getCoordinateID(region, uuid);
                Location l = globalSQL.getLocation(coordinateId);

                if (l == null) {
                    p.sendMessage(ChatUtils.error("An error occurred while fetching the location to teleport."));
                    log.warning("Location is null for coordinate id " + coordinateId);
                    return;
                }

                // Teleport player.
                p.teleport(l);
                p.sendMessage(ChatUtils.success("Teleported to region ").append(Component.text(regionManager.getTag(region, uuid), NamedTextColor.DARK_AQUA)));
            }
            case "server" -> // Switch to server.
                    serverAPI.switchServer(PlayerAdapter.adapt(p), event[2]);

            case "spawn" -> {

                // If server is Lobby, teleport to spawn.
                if (constants.serverType() == ServerType.LOBBY) {
                    p.teleport(lobby.getSpawn());
                    p.sendMessage(ChatUtils.success("Teleported to spawn."));
                } else {

                    // Set teleport event to go to spawn.
                    eventAPI.createTeleportEvent(true, p.getUniqueId().toString(), "teleport spawn", LocationAdapter.adapt(p.getLocation()));
                    serverAPI.switchServer(PlayerAdapter.adapt(p), globalSQL.getString("SELECT name FROM " + "server_data WHERE type='LOBBY';"));
                }
            }

            // Tpll command format.
            case "tpll" -> tpll.tpll(p, Arrays.copyOfRange(event, 2, event.length), true);

            default -> {

                // Get world.
                World world = WorldUtils.getWorld(event[1]);

                if (world == null) {
                    p.sendMessage(ChatUtils.error("World %s can not be found.", event[1]));
                    return;
                }

                double x;
                double y;
                double z;
                float yaw;
                float pitch;

                if (event.length == 6) {
                    // Length 6 means no y is specified.

                    // Get x and z.
                    x = Double.parseDouble(event[2]);
                    z = Double.parseDouble(event[3]);

                    // Get y elevation for teleport.
                    y = world.getHighestBlockYAt((int) x, (int) z);
                    y++;

                    // Get pitch and yaw.
                    yaw = Float.parseFloat(event[4]);
                    pitch = Float.parseFloat(event[5]);
                } else {
                    // Length 7 means y is specific.

                    // Get x, y and z.
                    x = Double.parseDouble(event[2]);
                    y = Double.parseDouble(event[3]);
                    z = Double.parseDouble(event[4]);

                    // Get pitch and yaw.
                    yaw = Float.parseFloat(event[5]);
                    pitch = Float.parseFloat(event[6]);
                }

                // Create location.
                Location l = new Location(world, x, y, z, yaw, pitch);

                // If the terrain has not been generated, let the player know it could take a while.
                if (!PaperLib.isChunkGenerated(l)) {
                    ChatUtils.success("Location is generating, please wait a moment...");
                }

                // Teleport player.
                PaperLib.teleportAsync(p, l);

                // If custom message is set, send that to player, else send default message.
                if (message == null) {
                    p.sendMessage(ChatUtils.success("Teleported to ").append(Component.text(DECIMAL_FORMATTER.format(x), NamedTextColor.DARK_AQUA)).append(ChatUtils.success(", "))
                            .append(Component.text(y, NamedTextColor.DARK_AQUA)).append(ChatUtils.success(", "))
                            .append(Component.text(DECIMAL_FORMATTER.format(z), NamedTextColor.DARK_AQUA)));
                } else {
                    p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
                }
            }
        }
    }
}
