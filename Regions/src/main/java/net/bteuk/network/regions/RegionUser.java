package net.bteuk.network.regions;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.core.Constants;
import org.bukkit.entity.Player;

import static net.bteuk.network.core.ServerType.EARTH;
import static net.bteuk.network.core.ServerType.PLOT;

@Log
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

        boolean bOnEarth = (constants.serverType() == EARTH && player.getWorld().getName().equals(constants.earthWorld()));
        boolean bOnPlot = constants.plotSystemEnabled() && (constants.serverType() == PLOT || constants.standalone());

        // Check if they are in the earth world
        if (bOnEarth) {
                trackedRegion = regionManager.getRegion(player.getLocation().getX(), player.getLocation().getZ());
                // Add the region to the database if not exists.
                regionManager.addToDatabase(trackedRegion);

        } else if (bOnPlot) {
            // Check if the player is in a buildable plot world and apply coordinate transform if true.
            if (plotAPI.hasLocation(player.getLocation().getWorld().getName())) {
                this.deltaX = -plotAPI.getXTransform(player.getLocation().getWorld().getName());
                this.deltaZ = -plotAPI.getZTransform(player.getLocation().getWorld().getName());

                trackedRegion = regionManager.getRegion(player.getLocation().getX(), player.getLocation().getZ(), deltaX, deltaZ);
            }
        }

        if (trackedRegion == null)
            log.info(player.getName() +" joined with no tracked region");
        else
            log.info(player.getName() +" joined with tracked region: " +trackedRegion.regionName());
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
