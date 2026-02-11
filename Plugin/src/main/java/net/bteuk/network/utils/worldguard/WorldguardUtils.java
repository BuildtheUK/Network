package net.bteuk.network.utils.worldguard;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.bteuk.network.exceptions.RegionManagerNotFoundException;
import net.bteuk.network.exceptions.RegionNotFoundException;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Optional;

public class WorldguardUtils {

    /**
     * Gets all the points of the region.
     *
     * @param regionName the region to get the points of
     * @param world      the world in which the region is
     * @return a list of {@link BlockVector2}
     * @throws RegionNotFoundException        if the region does not exist
     * @throws RegionManagerNotFoundException if the region manager does not exist for this world
     */
    public static List<BlockVector2> getPoints(String regionName, World world) throws RegionNotFoundException,
            RegionManagerNotFoundException {

        RegionManager buildRegions = WorldguardManager.getRegionManager(world);
        ProtectedPolygonalRegion region = (ProtectedPolygonalRegion) buildRegions.getRegion(regionName);

        if (region == null) {
            throw new RegionNotFoundException("Region " + regionName + " does not exist!");
        }

        return region.getPoints();
    }

    /**
     * See if the block is in a region.
     *
     * @param block the block to check
     * @return whether the block is in a region
     * @throws RegionManagerNotFoundException if no region manager exists for the world the block is in
     */
    public static boolean inRegion(Block block) throws RegionManagerNotFoundException {

        RegionManager regionManager = WorldguardManager.getRegionManager(block.getWorld());

        // Get the blockvector3 at the block.
        BlockVector3 v = BlockVector3.at(block.getX(), block.getY(), block.getZ());

        // Check whether the region overlaps an existing plot, if true stop the process.
        ApplicableRegionSet set = regionManager.getApplicableRegions(v);

        return set.size() > 0;
    }

    /**
     * Get a region at the location, or null if no regions can be found.
     *
     * @param world the {@link World} to check the location
     * @param bv3   the location as {@link BlockVector3}
     * @return the {@link ProtectedRegion}
     * @throws RegionManagerNotFoundException if no region manager exists for the world
     */
    public static ProtectedRegion getRegionAt(World world, BlockVector3 bv3) throws RegionManagerNotFoundException {

        RegionManager regionManager = WorldguardManager.getRegionManager(world);

        // Check whether there are any regions at this block.
        ApplicableRegionSet set = regionManager.getApplicableRegions(bv3);
        Optional<ProtectedRegion> optionalRegion = set.getRegions().stream().findFirst();

        return optionalRegion.orElse(null);
    }
}
