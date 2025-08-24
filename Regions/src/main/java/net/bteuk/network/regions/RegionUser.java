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
