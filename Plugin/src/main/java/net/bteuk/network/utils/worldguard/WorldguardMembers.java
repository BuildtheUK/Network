package net.bteuk.network.utils.worldguard;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.bteuk.network.exceptions.RegionManagerNotFoundException;
import net.bteuk.network.exceptions.RegionNotFoundException;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WorldguardMembers {

    public static boolean addMember(String regionName, String uuid,
                                    World world) throws RegionManagerNotFoundException, RegionNotFoundException {

        RegionManager buildRegions = WorldguardManager.getRegionManager(world);
        ProtectedRegion region = buildRegions.getRegion(regionName);

        if (region == null) {

            throw new RegionNotFoundException("Region " + regionName + " does not exist!");
        }

        // Add the member to the region.
        region.getMembers().addPlayer(UUID.fromString(uuid));

        // Save the changes
        try {
            buildRegions.saveChanges();
            return true;
        } catch (StorageException e1) {
            e1.printStackTrace();
            return false;
        }
    }

    public static void removeMember(String regionName, String uuid,
                                    World world) throws RegionManagerNotFoundException, RegionNotFoundException {

        RegionManager buildRegions = WorldguardManager.getRegionManager(world);
        ProtectedRegion region = buildRegions.getRegion(regionName);

        if (region == null) {

            throw new RegionNotFoundException("Region " + regionName + " does not exist!");
        }

        if (region.getMembers().contains(UUID.fromString(uuid))) {
            // Remove the member to the region.
            region.getMembers().removePlayer(UUID.fromString(uuid));
        } else {
            return;
        }

        // Save the changes
        try {
            buildRegions.saveChanges();
        } catch (StorageException e1) {
            e1.printStackTrace();
        }
    }

    public static void clearMembers(String regionName, World world) throws RegionNotFoundException,
            RegionManagerNotFoundException {

        RegionManager buildRegions = WorldguardManager.getRegionManager(world);
        ProtectedRegion region = buildRegions.getRegion(regionName);

        if (region == null) {

            throw new RegionNotFoundException("Region " + regionName + " does not exist!");
        }

        // Remove all members from the region.
        region.getMembers().clear();

        // Save the changes
        try {
            buildRegions.saveChanges();
        } catch (StorageException e1) {
            e1.printStackTrace();
        }
    }

    public static boolean isMemberOrOwner(ProtectedRegion region, Player p) {
        LocalPlayer player = WorldguardManager.wrapPlayer(p);
        return region.isMember(player);
    }
}
