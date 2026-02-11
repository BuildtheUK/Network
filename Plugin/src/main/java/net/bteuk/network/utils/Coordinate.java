package net.bteuk.network.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Representation of a coordinate database object.
 */
@AllArgsConstructor
@Data
public class Coordinate {

    /**
     * ID of the coordinate
     */
    private int id;

    private String server;

    private String world;

    private double x;

    private double y;

    private double z;

    private float yaw;

    private float pitch;

    /**
     * Return a location object for this coordinate.
     * If the world does not exist on this server it will return null!
     *
     * @return the location
     */
    public Location getLocation() {
        World world = Bukkit.getWorld(getWorld());
        if (world != null) {
            return new Location(world, getX(), getY(), getZ(), getYaw(), getPitch());
        }
        return null;
    }
}
