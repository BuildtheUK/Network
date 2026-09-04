package net.bteuk.network.regions.listener;

import lombok.extern.java.Log;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.core.Constants;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.papercore.WorldUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionStatus;
import net.bteuk.network.regions.RegionUser;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Log
public class RegionMoveListener extends AbstractMoveListener implements Listener {
    private final PlotAPI plotAPI;

    private final Constants constants;

    private final SQLAPI globalSQL;

    private final EventAPI eventAPI;

    private final ServerAPI serverAPI;

    public RegionMoveListener(JavaPlugin plugin, RegionManager regionManager, PlotAPI plotAPI, Constants constants, SQLAPI globalSQL, EventAPI eventAPI, ServerAPI serverAPI) {
        super(regionManager);

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        this.plotAPI = plotAPI;
        this.constants = constants;
        this.globalSQL = globalSQL;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {

        Optional<RegionUser> optionalRegionUser = regionManager.getUserByPlayer(e.getPlayer());
        if (optionalRegionUser.isEmpty()) {
            log.severe("Region user is null for player " + e.getPlayer().getName());
            return;
        }
        RegionUser regionUser = optionalRegionUser.get();

        // Check for movement between regions.
        // If the player is currently not in a region, then that implies they are in a world without regions, so
        // movement will not affect this.
        // Not being in a region also means that the region is null.
        if (regionUser.hasTrackedRegion()) {

            // Get x and z of the region as int rounded down with any necessary coordinate transforms.
            int x = ((e.getTo().getX() >= 0 ? (int) e.getTo().getX() : ((int) e.getTo().getX()) - 1) + regionUser.getDeltaX()) >> 9;
            int z = ((e.getTo().getZ() >= 0 ? (int) e.getTo().getZ() : ((int) e.getTo().getZ()) - 1) + regionUser.getDeltaZ()) >> 9;
            // These are the 'real' terra coords of the new region, regardless of where the user is

            // Check if the player has moved to another region.
            if (!regionUser.getTrackedRegion().equals(x, z)) {

                log.info(e.getPlayer().getName() + " is moving across a region border at coordinates " + e.getTo().getX() + "," + e.getTo().getZ());

                // Get the new region.
                Region newRegion = regionManager.getRegion(x, z);

                // Check if the new region is on this server or not. If it is, check whether it is on the same world.
                if (!constants.serverName().equals(regionManager.getServer(newRegion))) {

                    switchServer(regionUser, newRegion, e.getTo());
                    e.setCancelled(true);
                } else {

                    Location newLocation = e.getTo().clone();

                    // Get true coordinates (as on terra world)
                    double trueNewX = e.getTo().getX() + regionUser.getDeltaX();
                    double trueNewZ = e.getTo().getZ() + regionUser.getDeltaZ();

                    // Get the world that the region is in.
                    boolean isPlot = regionManager.isPlot(newRegion);
                    String world = isPlot ? plotAPI.getRegionLocation(newRegion.regionName()) : constants.earthDimension();

                    if (!newLocation.getWorld().key().asMinimalString().equals(world)) {
                        if (isPlot) {
                            // Apply new region shift
                            String szLocation = plotAPI.getRegionLocation(newRegion.regionName());
                            trueNewX = trueNewX + plotAPI.getXTransform(szLocation);
                            trueNewZ = trueNewZ + plotAPI.getZTransform(szLocation);
                        }
                        newLocation.setWorld(WorldUtils.getWorld(world));
                        newLocation.setX(trueNewX);
                        newLocation.setZ(trueNewZ);
                        e.setTo(newLocation);
                    }

                    e.setCancelled(switchRegion(regionUser, newRegion));
                }
            }
        }
    }

    private void switchServer(RegionUser regionUser, Region region, Location newLocation) {
        // Check if the player can enter the region.
        if (regionManager.inDatabase(region) || regionUser.getPlayer().hasPermission("uknet.regions.generate")) {
            // If cross-server teleport is enabled teleport them to the correct server and location.
            if (constants.standalone()) {
                // Cancel movement as the location is on another server, but the server doesn't support multiple servers.
                regionUser.getPlayer().sendMessage(ChatUtils.error("The terrain for this location is on another server, you may " +
                        "not enter."));
            } else {
                // If the server is offline, notify the player.
                if (globalSQL.hasRow("SELECT 1 FROM server_data WHERE online=1 AND name='" + regionManager.getServer(region) + "';")) {

                    // Add the region to the database if not exists.
                    regionManager.addToDatabase(region);

                    // Region is on another server, teleport them accordingly.
                    // If the new region is on a plot server, check for coordinate transform.
                    String world = constants.earthDimension();
                    int xTransform = regionUser.getDeltaX();
                    int zTransform = regionUser.getDeltaZ();
                    if (constants.plotSystemEnabled() && regionManager.status(region) == RegionStatus.PLOT) {
                        // Get server and world of the region.
                        String location = plotAPI.getRegionLocation(region.regionName());

                        xTransform = plotAPI.getXTransform(location);
                        zTransform = plotAPI.getZTransform(location);

                        world = location;
                    }

                    // Set join event to teleport there.
                    eventAPI.createJoinEvent(regionUser.getPlayer().getUniqueId().toString(),
                            "teleport " + world + " " + (newLocation.getX() + xTransform) + " " + (newLocation.getZ() + zTransform) + " " + newLocation.getYaw() + " " + newLocation.getPitch());

                    // Switch server.
                    serverAPI.switchServer(PlayerAdapter.adapt(regionUser.getPlayer()), regionManager.getServer(region));
                } else {
                    regionUser.getPlayer().sendMessage(ChatUtils.error("This region is on another server, however the server is currently offline."));
                }
            }
        } else {
            // You can't enter this region.
            regionUser.getPlayer().sendMessage(ChatUtils.error("The terrain for this region has not been generated, you " +
                    "do not have permission load new terrain."));
        }
    }
}
