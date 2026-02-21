package net.bteuk.network.regions.listener;

import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionStatus;
import net.bteuk.network.regions.RegionUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public abstract class AbstractMoveListener {

    protected final RegionManager regionManager;

    protected AbstractMoveListener(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    /**
     * Switch the user from one region to another.
     *
     * @param regionUser    the region user
     * @param newRegion     the region the player is moving to
     * @return boolean whether to cancel the movement event
     */
    protected boolean switchRegion(RegionUser regionUser, Region newRegion) {

        // Implies that the user has left their current region.
        if (newRegion == null) {
            // Send default leave message.
            regionUser.getPlayer().sendActionBar(
                    ChatUtils.success("You have left ")
                            .append(Component.text(regionManager.getTag(regionUser.getTrackedRegion(), regionUser.getPlayer().getUniqueId().toString()), NamedTextColor.DARK_AQUA)));

            // Set inRegion to false.
            regionUser.setTrackedRegion(null);
            return false;
        }

        // Check if the player can enter the region.
        if (regionManager.inDatabase(newRegion) || regionUser.getPlayer().hasPermission("uknet.regions.generate")) {
            // Add the region to the database if not exists.
            regionManager.addToDatabase(newRegion);

            // If the player is the region owner update the last enter time and send the message.
            if (regionManager.isOwner(newRegion, regionUser.getPlayer().getUniqueId().toString())) {

                sendRegionOwnerEnterMessage(regionUser, newRegion);
                regionManager.setLastEnter(newRegion, regionUser.getPlayer().getUniqueId().toString());

                // If the region is inactive, set it to active.
                if (regionManager.status(newRegion) == RegionStatus.INACTIVE) {
                    regionManager.setDefault(newRegion);
                    regionUser.getPlayer().sendMessage(ChatUtils.success("This region is no longer \"Inactive\", it has been " +
                            "set back to default settings."));
                }

                // Check if the player is a region member.
            } else if (regionManager.isMember(newRegion, regionUser.getPlayer().getUniqueId().toString())) {

                sendRegionMemberEnterMessage(regionUser, newRegion);
                regionManager.setLastEnter(newRegion, regionUser.getPlayer().getUniqueId().toString());

                // If the region is inactive, make this member to owner.
                if (regionManager.status(newRegion) == RegionStatus.INACTIVE) {
                    // Make the previous owner a member.
                    regionManager.makeMember(newRegion);

                    // Give the new player ownership.
                    regionManager.makeOwner(newRegion, regionUser.getPlayer().getUniqueId().toString());

                    // Update any requests to take into account the new region owner.
                    regionManager.updateRequests(newRegion);

                    regionUser.getPlayer().sendMessage(ChatUtils.success("This region is no longer \"Inactive\", it has been " +
                            "set back to default settings."));
                    regionUser.getPlayer().sendMessage(ChatUtils.success("You have been made the new region owner."));
                }

                // Check if the region is open and the player is at least jr.builder.
            } else if (regionManager.status(newRegion) == RegionStatus.OPEN && regionUser.getPlayer().hasPermission("group.jrbuilder")) {
                sendOpenRegionEnterMessage(regionUser, newRegion);
            } else {

                // Send the default region entered message.
                sendDefaultRegionEnterMessage(regionUser, newRegion);
            }

            // Update the region the player is in.
            regionUser.setTrackedRegion(newRegion);
            return false;
        } else {

            // You can't enter this region.
            regionUser.getPlayer().sendMessage(ChatUtils.error("The terrain for this region has not been generated, " +
                    "you do not have permission to load new terrain."));
            return true;
        }
    }

    private void sendRegionOwnerEnterMessage(RegionUser regionUser, Region newRegion) {
        Component message = getEnteredMessage(regionUser, newRegion);
        message = message.append(ChatUtils.success(", you are the owner of this region."));
        regionUser.getPlayer().sendActionBar(message);
    }

    private void sendRegionMemberEnterMessage(RegionUser regionUser, Region newRegion) {
        Component message = getEnteredMessage(regionUser, newRegion);
        message = message.append(ChatUtils.success(", you are a member of this region."));
        regionUser.getPlayer().sendActionBar(message);
    }

    private void sendOpenRegionEnterMessage(RegionUser regionUser, Region newRegion) {
        Component message = getEnteredMessage(regionUser, newRegion);
        message = message.append(ChatUtils.success(", you can build in this region."));
        regionUser.getPlayer().sendActionBar(message);
    }

    private void sendDefaultRegionEnterMessage(RegionUser regionUser, Region newRegion) {
        regionUser.getPlayer().sendActionBar(getEnteredMessage(regionUser, newRegion));
    }

    private Component getEnteredMessage(RegionUser regionUser, Region newRegion) {
        Component message = ChatUtils.success("You have entered ").append(Component.text(regionManager.getTag(newRegion, regionUser.getPlayer().getUniqueId().toString()), NamedTextColor.DARK_AQUA));
        if (regionUser.hasTrackedRegion()) {
            message = message.append(ChatUtils.success(" and left ").append(Component.text(regionManager.getTag(regionUser.getTrackedRegion(), regionUser.getPlayer().getUniqueId().toString()), NamedTextColor.DARK_AQUA)));
        }
        return message;
    }
}
