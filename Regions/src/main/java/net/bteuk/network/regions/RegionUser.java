package net.bteuk.network.regions;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

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

    public RegionUser(Player player) {
        this.player = player;

        // TODO: Copied from NetworkUser, implement this.
        // Check if the player is in a region.
        if (constants.regionsEnabled()) {
            if (SERVER_TYPE == EARTH) {
                // Check if they are in the earth world.
                if (player.getWorld().getName().equals(EARTH_WORLD)) {
                    region = instance.getRegionManager().getRegion(player.getLocation());
                    // Add region to database if not exists.
                    region.addToDatabase();
                    inRegion = true;
                }
            } else if (SERVER_TYPE == PLOT) {
                // Check if the player is in a buildable plot world and apply coordinate transform if true.
                if (instance.getPlotSQL()
                        .hasRow("SELECT name FROM location_data WHERE name='" + player.getLocation().getWorld()
                                .getName() + "';")) {
                    updateCoordinateTransform(instance.getPlotSQL(), player.getLocation());

                    region = instance.getRegionManager().getRegion(player.getLocation(), dx, dz);
                    inRegion = true;
                }
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
