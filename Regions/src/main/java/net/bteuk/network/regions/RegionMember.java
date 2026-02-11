package net.bteuk.network.regions;

/**
 * Representation of an entry in the region_members table.
 */
public record RegionMember(String region, String uuid, boolean isOwner, long lastEnter, String tag, int coordinateId,
                           boolean pinned) {
    public String getTag() {
        if (tag == null) {
            return region;
        }
        return tag;
    }
}
