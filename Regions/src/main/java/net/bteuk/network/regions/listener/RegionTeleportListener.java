package net.bteuk.network.regions.listener;

import lombok.extern.java.Log;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.core.Constants;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionUser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

import static net.bteuk.network.core.ServerType.EARTH;
import static net.bteuk.network.core.ServerType.PLOT;

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
    public void onPlayerTeleport(PlayerTeleportEvent e) {

        Optional<RegionUser> optionalRegionUser = regionManager.getUserByPlayer(e.getPlayer());
        if (optionalRegionUser.isEmpty()) {
            log.severe("Region user is null for player " + e.getPlayer().getName());
            return;
        }
        RegionUser regionUser = optionalRegionUser.get();

        Region newRegion = getRegion(regionUser, e.getTo());

        if (!Objects.equals(newRegion, regionUser.getTrackedRegion())) {
            e.setCancelled(switchRegion(regionUser, newRegion));
        }
    }

    private Region getRegion(RegionUser regionUser, Location location) {
        Region region = null;
        if (constants.serverType() == EARTH) {
            if (location.getWorld().getName().equals(constants.earthWorld())) {
                // Get region.
                region = regionManager.getRegion(location.getX(), location.getZ());
            }
        } else if (constants.plotSystemEnabled() && constants.serverType() == PLOT) {
            // Check if the player is teleporting to a buildable world in the plot system.
            if (plotAPI.hasLocation(location.getWorld().getName())) {

                // Get negative coordinate transform of new location.
                int deltaX = -plotAPI.getXTransform(location.getWorld().getName());
                int deltaZ = -plotAPI.getZTransform(location.getWorld().getName());

                regionUser.setDeltaX(deltaX);
                regionUser.setDeltaZ(deltaZ);

                // Get the region with coordinate transformation.
                region = regionManager.getRegion(location.getX() + deltaX, location.getZ() + deltaZ);
            }
        }
        return region;
    }
}
