package net.bteuk.network.utils.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import lombok.extern.java.Log;
import net.bteuk.network.api.WorldGuardAPI;
import org.bukkit.World;

import java.util.UUID;

@Log
public class WorldGuard implements WorldGuardAPI {

    public boolean addMember(String regionName, String uuid, String world) {

        RegionManager regionManager = getRegionManager(world);

        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = getRegion(regionManager, regionName);

        if (region == null) {
            return false;
        }

        // Add the member to the region.
        region.getMembers().addPlayer(UUID.fromString(uuid));

        // Save the changes
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException e1) {
            return false;
        }
    }

    public boolean addGroup(String regionName, String group, String world) {

        RegionManager regionManager = getRegionManager(world);

        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = getRegion(regionManager, regionName);

        if (region == null) {
            return false;
        }

        // Add the group to the region.
        region.getMembers().addGroup(group);
        log.info("Added " + group + " to " + region);

        // Save the changes
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException e1) {
            return false;
        }
    }

    public boolean removeMember(String regionName, String uuid, String world) {

        RegionManager regionManager = getRegionManager(world);

        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = getRegion(regionManager, regionName);

        if (region == null) {
            return false;
        }

        // Check if the member is in the region.
        if (region.getMembers().contains(UUID.fromString(uuid))) {
            // Remove the member to the region.
            region.getMembers().removePlayer(UUID.fromString(uuid));
        } else {
            return false;
        }

        // Save the changes
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException e1) {
            return false;
        }
    }

    public boolean removeGroup(String regionName, String group, String world) {

        RegionManager regionManager = getRegionManager(world);

        if (regionManager == null) {
            return false;
        }

        ProtectedRegion region = getRegion(regionManager, regionName);

        if (region == null) {
            return false;
        }

        region.getMembers().removeGroup(group);

        // Save the changes
        try {
            regionManager.saveChanges();
            return true;
        } catch (
                StorageException e1) {
            return false;
        }
    }

    public boolean createRegion(String regionName, int xmin, int zmin, int xmax, int zmax, String world) {

        RegionManager regionManager = getRegionManager(world);

        if (regionManager == null) {
            return false;
        }

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionName, BlockVector3.at(xmin, -512, zmin),
                BlockVector3.at(xmax, 1536, zmax));

        regionManager.addRegion(region);

        // Save the changes
        try {
            regionManager.saveChanges();
            return true;
        } catch (StorageException e1) {
            return false;
        }
    }

    private static RegionManager getRegionManager(String world) {
        // Get an instance of WorldGuard.
        com.sk89q.worldguard.WorldGuard wg = com.sk89q.worldguard.WorldGuard.getInstance();

        World bukkitWorld = org.bukkit.Bukkit.getWorld(world);

        if (bukkitWorld == null) {
            log.warning("World " + world + " does not exist!");
            return null;
        }

        // Get regions.
        RegionContainer container = wg.getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(bukkitWorld));

        if (regionManager == null) {
            log.warning("RegionManager for world " + world + " is null!");
        }

        return regionManager;
    }

    private static ProtectedRegion getRegion(RegionManager regionManager, String regionName) {

        // Remove the group from the region.
        ProtectedRegion buildRegion = regionManager.getRegion(regionName);

        if (buildRegion == null) {
            log.warning("Region " + regionName + " does not exist!");
        }

        return buildRegion;
    }
}