package net.bteuk.network.regions;

import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.core.Constants;
import org.bukkit.entity.Player;

import static net.bteuk.network.core.ServerType.EARTH;
import static net.bteuk.network.core.ServerType.PLOT;

public class RegionUser {

    @Getter
    private final Player player;

    // The region that the player was last known to be in.
    @Setter
    @Getter
    private Region trackedRegion;

    // Coordinate transformation in the x direction, relevant in the plot system.
    @Getter
    @Setter
    int deltaX;

    // Coordinate transformation in the z direction, relevant in the plot system.
    @Getter
    @Setter
    int deltaZ;

    public RegionUser(Player player, Constants constants, RegionManager regionManager, PlotAPI plotAPI) {
        this.player = player;

        // Check if the player is in a region.
        if (constants.serverType() == EARTH) {
            // Check if they are in the earth world.
            if (player.getWorld().getName().equals(constants.earthWorld())) {
                trackedRegion = regionManager.getRegion(player.getLocation().getX(), player.getLocation().getZ());
                // Add the region to the database if not exists.
                regionManager.addToDatabase(trackedRegion);
            }
        } else if (constants.plotSystemEnabled() && constants.serverType() == PLOT) {
            // Check if the player is in a buildable plot world and apply coordinate transform if true.
            if (plotAPI.hasLocation(player.getLocation().getWorld().getName())) {
                this.deltaX = -plotAPI.getXTransform(player.getLocation().getWorld().getName());
                this.deltaZ = -plotAPI.getZTransform(player.getLocation().getWorld().getName());

                trackedRegion = regionManager.getRegion(player.getLocation().getX(), player.getLocation().getZ(), deltaX, deltaZ);
            }
        }
    }

    public boolean hasTrackedRegion() {
        return trackedRegion != null;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RegionUser u) {
            return u.player.equals(player);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }
}
