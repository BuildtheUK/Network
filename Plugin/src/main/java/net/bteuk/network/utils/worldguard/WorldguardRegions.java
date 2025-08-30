package net.bteuk.network.utils.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import net.bteuk.network.exceptions.RegionManagerNotFoundException;
import org.bukkit.World;

public class WorldguardRegions {

    public static boolean delete(String regionName, World world) throws RegionManagerNotFoundException {

        // Get instance of WorldGuard.
        WorldGuard wg = WorldGuard.getInstance();

        // Get regions.
        RegionContainer container = wg.getPlatform().getRegionContainer();
        RegionManager buildRegions = container.get(BukkitAdapter.adapt(world));

        if (buildRegions == null) {

            throw new RegionManagerNotFoundException("RegionManager for world " + world.getName() + " is null!");
        }

        // Get the region to remove the outlines.
        ProtectedRegion region = buildRegions.getRegion(regionName);

        // Attempt to remove the plot.
        buildRegions.removeRegion(regionName);

        // Save the changes
        try {
            buildRegions.saveChanges();
            return true;
        } catch (StorageException e1) {
            e1.printStackTrace();
            return false;
        }
    }
}
