package net.bteuk.network.papercore;

import net.bteuk.network.api.entity.NetworkLocation;
import org.bukkit.Location;

public class LocationAdapter {

    public static NetworkLocation adapt(Location location) {
        return new NetworkLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
