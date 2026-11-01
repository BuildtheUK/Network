package net.bteuk.network.waypoint;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.core.Constants;
import net.bteuk.network.socket.MessageSender;
import org.btuk.network.lib.dto.WaypointHologramRequest;
import org.btuk.network.lib.dto.WaypointHologramUpdateEvent;
import org.btuk.network.lib.dto.WaypointLocationRequest;
import org.btuk.network.lib.dto.WaypointLocationResponse;
import org.btuk.network.lib.dto.WaypointTeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.UUID;

@Log
public class NetworkWaypointManager implements Listener {

    private final Network instance;
    private final Constants constants;
    private final MessageSender messageSender;

    private static final String HOLOGRAM_NAME = "waypoint_lobby";
    private boolean hologramInitialized = false;
    private boolean hologramActive = false;

    public NetworkWaypointManager(Network instance, Constants constants, MessageSender messageSender) {
        this.instance = instance;
        this.constants = constants;
        this.messageSender = messageSender;

        Bukkit.getServer().getPluginManager().registerEvents(this, instance);

        // Request initial hologram data from proxy after delay
        Bukkit.getScheduler().runTaskLater(instance, this::requestHologramData, 60L);

        // Periodic task to ensure hologram recovery if proxy or server was offline
        Bukkit.getScheduler().runTaskTimer(instance, () -> {
            // If DecentHolograms is installed and hologram is either not initialized yet or active DH hologram is missing, request again
            if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
                if (!hologramInitialized || (hologramActive && DHAPI.getHologram(HOLOGRAM_NAME) == null)) {
                    requestHologramData();
                }
            }
        }, 1200L, 1200L); // Check every 60 seconds (1200 ticks)
    }

    public void requestHologramData() {
        messageSender.sendSocketMessage(new WaypointHologramRequest(constants.serverName()));
    }

    public void handleLocationRequest(WaypointLocationRequest request) {
        if (request.getPlayerUuid() == null) return;

        UUID uuid;
        try {
            uuid = UUID.fromString(request.getPlayerUuid());
        } catch (IllegalArgumentException e) {
            messageSender.sendSocketMessage(new WaypointLocationResponse(request.getPlayerUuid(), null, 0, 0, 0, false, "Invalid player UUID"));
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            messageSender.sendSocketMessage(new WaypointLocationResponse(request.getPlayerUuid(), null, 0, 0, 0, false, "Player not online on this server"));
            return;
        }

        Location loc = player.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "";
        messageSender.sendSocketMessage(new WaypointLocationResponse(request.getPlayerUuid(), worldName, loc.getX(), loc.getY(), loc.getZ(), true, null));
    }

    public void handleTeleportEvent(WaypointTeleportEvent event) {
        if (event.getPlayerUuid() == null) return;

        UUID uuid;
        try {
            uuid = UUID.fromString(event.getPlayerUuid());
        } catch (IllegalArgumentException e) {
            return;
        }

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        World world = Bukkit.getWorld(event.getWorld());
        if (world == null) {
            log.warning("Waypoint teleport failed: World '" + event.getWorld() + "' not found.");
            return;
        }

        Location loc = new Location(world, event.getX(), event.getY(), event.getZ(), event.getYaw(), event.getPitch());
        Bukkit.getScheduler().runTask(instance, () -> player.teleport(loc));
    }

    public void handleHologramUpdate(WaypointHologramUpdateEvent event) {
        if (event == null) return;

        // Check if DecentHolograms is installed
        if (!Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            return;
        }

        // Check if event targets this server (or server matches)
        if (event.getServer() != null && !event.getServer().equalsIgnoreCase(constants.serverName())) {
            // Not for this server, remove hologram if exists
            Bukkit.getScheduler().runTask(instance, () -> {
                if (DHAPI.getHologram(HOLOGRAM_NAME) != null) {
                    DHAPI.removeHologram(HOLOGRAM_NAME);
                }
                hologramInitialized = true;
                hologramActive = false;
            });
            return;
        }

        Bukkit.getScheduler().runTask(instance, () -> {
            try {
                if (!event.isEnabled()) {
                    if (DHAPI.getHologram(HOLOGRAM_NAME) != null) {
                        DHAPI.removeHologram(HOLOGRAM_NAME);
                    }
                    hologramInitialized = true;
                    hologramActive = false;
                    return;
                }

                World world = Bukkit.getWorld(event.getWorld());
                if (world == null) {
                    return;
                }

                Location loc = new Location(world, event.getX(), event.getY(), event.getZ());
                List<String> lines = event.getLines();

                Hologram hologram = DHAPI.getHologram(HOLOGRAM_NAME);
                if (hologram == null) {
                    DHAPI.createHologram(HOLOGRAM_NAME, loc, lines);
                } else {
                    DHAPI.moveHologram(HOLOGRAM_NAME, loc);
                    DHAPI.setHologramLines(hologram, lines);
                }
                hologramInitialized = true;
                hologramActive = true;
            } catch (Exception e) {
                log.warning("Failed to update waypoint lobby hologram: " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onHologramClick(eu.decentsoftware.holograms.event.HologramClickEvent event) {
        if (HOLOGRAM_NAME.equalsIgnoreCase(event.getHologram().getName())) {
            event.getPlayer().performCommand("waypoint");
        }
    }
}
