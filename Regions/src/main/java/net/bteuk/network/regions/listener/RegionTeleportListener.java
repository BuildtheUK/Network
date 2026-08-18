package net.bteuk.network.regions.listener;

import lombok.extern.java.Log;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionUser;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Log
public class RegionTeleportListener extends AbstractMoveListener implements Listener {

    private final Constants constants;

    private final PlotAPI plotAPI;

    public RegionTeleportListener(JavaPlugin instance, RegionManager regionManager, Constants constants, PlotAPI plotAPI) {
        super(regionManager);

        Bukkit.getServer().getPluginManager().registerEvents(this, instance);

        this.constants = constants;
        this.plotAPI = plotAPI;
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent e) {

        log.info("Player World Change Detected");

        Optional<RegionUser> optionalRegionUser = regionManager.getUserByPlayer(e.getPlayer());
        if (optionalRegionUser.isEmpty()) {
            log.severe("Region user is null for player " + e.getPlayer().getName());
            return;
        }

        log.info("Previous player world: " + e.getFrom().key().asMinimalString());
        RegionUser regionUser = optionalRegionUser.get();
        log.info("Previous player deltas: (" + regionUser.getDeltaX() + "," + regionUser.getDeltaZ() + ")");

        // Update player delta if standalone
        if (constants.serverType() == ServerType.PLOT || constants.standalone()) {

            log.info("Updating player deltas because in plotsystem or standalone");
            String szWorldName = e.getPlayer().getWorld().key().asMinimalString();
            log.info("New world: " + szWorldName);
            if (!szWorldName.equals(constants.earthDimension())) {
                int deltaX = -plotAPI.getXTransform(szWorldName);
                int deltaZ = -plotAPI.getZTransform(szWorldName);

                regionUser.setDeltaX(deltaX);
                regionUser.setDeltaZ(deltaZ);
            } else {
                regionUser.setDeltaX(0);
                regionUser.setDeltaZ(0);
            }
            log.info("New player deltas: (" + regionUser.getDeltaX() + "," + regionUser.getDeltaZ() + ")");
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {

        log.fine("Player Teleport Event detected via " + e.getCause().name() + ". Attempting region switch");

        Optional<RegionUser> optionalRegionUser = regionManager.getUserByPlayer(e.getPlayer());
        if (optionalRegionUser.isEmpty()) {
            log.severe("Region user is null for player " + e.getPlayer().getName());
            return;
        }
        RegionUser regionUser = optionalRegionUser.get();

        // If plot world, get transformation
        String szNewWorld = e.getTo().getWorld().key().asMinimalString();
        int addX = 0;
        int addZ = 0;
        if (plotAPI.hasLocation(szNewWorld)) {
            addX = -plotAPI.getXTransform(szNewWorld);
            addZ = -plotAPI.getZTransform(szNewWorld);
        }

        // Get x and z of the region as int rounded down.
        int terraX = ((e.getTo().getX() >= 0 ? (int) e.getTo().getX() : ((int) e.getTo().getX()) - 1) + addX) >> 9;
        int terraZ = ((e.getTo().getZ() >= 0 ? (int) e.getTo().getZ() : ((int) e.getTo().getZ()) - 1) + addZ) >> 9;

        if (!regionUser.hasTrackedRegion()) {
            log.fine(e.getPlayer().getName() + " does not have a current tracked region, attempting a fix");
        } else if (!regionUser.getTrackedRegion().equals(terraX, terraZ)) {
            log.fine(e.getPlayer().getName() + " is teleporting across a region border");
        }

        // Get the new region.
        Region newRegion = regionManager.getRegion(terraX, terraZ);

        if (newRegion == null) {
            log.info("New region of " + regionUser.getPlayer().getName() + " is null");
        } else if (newRegion.equals(regionUser.getTrackedRegion())) {
            // Region is the same, do nothing.
            return;
        } else {
            log.info("New region of " + regionUser.getPlayer().getName() + " is " + newRegion.regionName());
        }

        e.setCancelled(switchRegion(regionUser, newRegion));
    }
}