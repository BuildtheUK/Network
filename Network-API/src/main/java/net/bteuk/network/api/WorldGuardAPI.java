package net.bteuk.network.api;

public interface WorldGuardAPI {

    boolean addMember(String regionName, String uuid, String world);

    boolean removeMember(String regionName, String uuid, String world);

    boolean addGroup(String regionName, String groupName, String world);

    boolean removeGroup(String regionName, String groupName, String world);

    boolean createRegion(String regionName, int xMin, int zMin, int xMax, int zMax, String world);
}
