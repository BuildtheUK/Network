package net.bteuk.network.utils.worldguard;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.core.math.Point;
import net.bteuk.network.exceptions.RegionManagerNotFoundException;
import net.bteuk.network.exceptions.RegionNotFoundException;
import net.bteuk.network.utils.Utils;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Worldguard functions specific to the plot system.
 */
public class WorldguardPlotsystem {

    private final PlotAPI plotAPI;

    public WorldguardPlotsystem(PlotAPI plotAPI) {
        this.plotAPI = plotAPI;
    }

    /**
     * Get the points of a specific plot or zone as if it was located in the save world.
     * This is done by getting the points in the world where the plot or zone is and then applying the negative
     * transform from its original location.
     *
     * @param regionName the name of the plot or zone
     * @param world      the name of the world where the plot or zone exists, NOT the world of the save world
     */
    public List<BlockVector2> getPointsTransformedToSaveWorld(String regionName,
                                                                     World world) throws RegionNotFoundException,
            RegionManagerNotFoundException {

        List<BlockVector2> vector = WorldguardUtils.getPoints(regionName, world);
        List<BlockVector2> newVector = new ArrayList<>();

        // Get the negative coordinate transform.
        int xTransform = -plotAPI.getXTransform(world.key().asMinimalString());
        int zTransform = -plotAPI.getZTransform(world.key().asMinimalString());

        // Apply to transform to each coordinate.
        vector.forEach(bv -> newVector.add(BlockVector2.at(bv.x() + xTransform, bv.z() + zTransform)));

        return newVector;
    }

    /**
     * Get the location of the centre of a region.
     *
     * @param regionName the region to get the location of
     * @param world      the world in which the region is
     * @return the {@link Location} of the centre of the region
     * @throws RegionNotFoundException        if the region can not be found
     * @throws RegionManagerNotFoundException if no region manager exists for this world
     */
    public Location getCurrentLocation(String regionName, World world) throws RegionNotFoundException,
            RegionManagerNotFoundException {

        // Get the region manager.
        RegionManager regionManager = WorldguardManager.getRegionManager(world);

        // Get the worldguard region and teleport to player to one of the corners.
        ProtectedPolygonalRegion region = (ProtectedPolygonalRegion) regionManager.getRegion(regionName);

        if (region == null) {
            throw new RegionNotFoundException("Region " + regionName + " does not exist!");
        }

        double[] averagePoint = Point.getAveragePoint(region.getPoints().stream().map(blockVector2 -> new double[]{blockVector2.x(), blockVector2.z()}).toList());

        return (new Location(world, averagePoint[0], Utils.getHighestYAt(world, (int) averagePoint[0], (int) averagePoint[1]), averagePoint[1]));
    }
}
