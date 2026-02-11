package net.bteuk.network.utils;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.bteuk.network.core.math.Point;
import net.bteuk.network.exceptions.building_companion.DistanceLimitException;
import net.bteuk.network.exceptions.building_companion.OutsidePlotException;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.round;

public class Blocks {

    private static final int MAX_DISTANCE = 200;

    // Add a line.
    public static void drawLine(Player player, World world, int[] block1, int[] block2, BlockData block,
                                boolean permanent, boolean skipFirst,
                                ProtectedRegion checkRegionPermission) throws OutsidePlotException,
            DistanceLimitException {

        // Check for maximum distance.
        if (Point.distanceBetween(block1, block2) > MAX_DISTANCE) {
            throw new DistanceLimitException("The distance between 2 corners too big, cancelled drawing outlines.");
        }

        // Get length in x and z direction.
        int lengthX = block2[0] - block1[0];
        int lengthZ = block2[1] - block1[1];

        int length = max(abs(lengthX), abs(lengthZ));

        // Iterate over the largest length of the two.
        for (int i = 0; i <= length; i++) {
            if (skipFirst && i == 0) {
                continue;
            }
            // Remove the points from the list.
            int x = (int) (round(block1[0] + ((i * lengthX) / (double) length)));
            int z = (int) (round(block1[1] + ((i * lengthZ) / (double) length)));
            // Check permission.
            if (checkRegionPermission == null || checkRegionPermission.contains(x, 1, z)) {
                drawBlock(player, world, x, z, block, permanent);
            } else {
                throw new OutsidePlotException("All or part of your selection is not in a plot you can build in, " +
                        "cancelled drawing outlines.");
            }
        }
    }

    // Draw a specific block.
    private static void drawBlock(Player player, World world, int x, int z, BlockData block, boolean permanent) {
        Location l = new Location(world, x, (world.getHighestBlockYAt(x, z) + 1), z);
        if (permanent) {
            world.setBlockData(l, block);
        } else {
            player.sendBlockChange(l, block);
        }
    }
}
