package net.bteuk.network.papercore;

import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

@Log
public class WorldUtils {

    private WorldUtils() {
        // Private constructor.
    }

    public static World getWorld(String dimension) {
        if (dimension == null) {
            return null;
        }
        // Dimension keys must always be lowercase;
        String lowerCaseDimension = dimension.toLowerCase();
        if (!dimension.equals(lowerCaseDimension)) {
            log.warning("Dimension key must be lowercase: " + dimension);
        }
        NamespacedKey key = NamespacedKey.fromString(dimension);
        if (key == null) {
            log.severe("Invalid dimension: " + dimension);
            return null;
        }
        World world = Bukkit.getWorld(key);
        if (world == null) {
            log.severe("Invalid world: " + dimension);
            return null;
        }
        return world;
    }
}
