package net.bteuk.network.building_companion;

import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

/**
 * Class to represent a saved outline.
 */
public record SavedOutline(UUID uuid, int[][] corners, World world) {
    @Override
    public boolean equals(Object o) {
        if (o instanceof UUID oUuid) {
            return Objects.equals(uuid, oUuid);
        } else {
            return Objects.equals(this, o);
        }
    }
}
