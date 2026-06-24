package net.bteuk.network.regions;

import net.bteuk.network.api.ChatAPI;
import net.bteuk.network.api.CoordinateAPI;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.api.entity.Event;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class RegionEvent implements Event {

    private final RegionManager regionManager;
    private final ChatAPI chatAPI;
    private final SQLAPI globalSQL;
    private final CoordinateAPI coordinateAPI;

    public RegionEvent(RegionManager regionManager, ChatAPI chatAPI, SQLAPI globalSQL, CoordinateAPI coordinateAPI) {
        this.regionManager = regionManager;
        this.chatAPI = chatAPI;
        this.globalSQL = globalSQL;
        this.coordinateAPI = coordinateAPI;
    }

    @Override
    public void event(String uuid, String[] event, String eMessage) {

        Region region;

        switch (event[1]) {
            case "set" -> {

                // Get region.
                region = regionManager.getRegion(event[3]);

                // If the region is not in the database, add it.
                regionManager.addToPlotsystem(region);
                if (event[2].equals("plotsystem")) {

                    // If region is not already set to plotsystem.
                    if (!(regionManager.status(region) == RegionStatus.PLOT)) {

                        // Set region to plotsystem.
                        // This will kick any members.
                        regionManager.setPlot(region, chatAPI);
                    }
                } else if (event[2].equals("default")) {

                    // If region is not already set to default.
                    if (!(regionManager.status(region) == RegionStatus.DEFAULT)) {

                        // Set region to default.
                        regionManager.setDefault(region);
                    }
                }
            }
            case "request" -> {
                if (event[2].equals("accept")) {

                    // 'region', request, accept/deny, region, uuid, owner/staff

                    region = regionManager.getRegion(event[3]);

                    // If length is 4, then no user is specified; this implies that it should accept all requests for
                    // the region, rather than a specific request.
                    if (event.length == 4) {

                        regionManager.acceptRequests(region);
                    } else {
                        RegionManager.RequestType requestType;

                        // The 6th argument specifies whether this is staff or owner approval
                        if (event[5].equals("owner"))
                            requestType = RegionManager.RequestType.OWNER;
                        else if (event[5].equals("staff"))
                            requestType = RegionManager.RequestType.STAFF;
                        else
                        {
                            DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid, "server",
                                    ChatUtils.error("An error occurred - the request type was malformatted: "+event[5]), true);
                            chatAPI.sendDirectMessage(directMessage);
                            return;
                        }

                        // The 5th argument specifies the uuid of the player who created the request.
                        regionManager.acceptRequest(region, event[4], requestType);

                        // Send feedback to the user who accepted the request.
                        DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid, "server",
                                ChatUtils.success("Accepted region request for %s in the region %s.",
                                        globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", event[4]), event[3]), true);
                        chatAPI.sendDirectMessage(directMessage);
                    }
                } else if (event[2].equals("deny")) {

                    region = regionManager.getRegion(event[3]);

                    regionManager.denyRequest(region, event[4]);

                    // Send feedback to the user who denied the request.
                    DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid, "server",
                            ChatUtils.success("Denied region request for %s in the region %s.",
                                    globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", event[4]), event[3]), true);
                    chatAPI.sendDirectMessage(directMessage);
                }
            }
            case "leave" -> {

                // Get region.
                region = regionManager.getRegion(event[2]);

                // Leave region.
                regionManager.leaveRegion(region, uuid, LegacyComponentSerializer.legacyAmpersand().deserialize(eMessage));

                // If the region has members after you've left but no owner.
                // Find the most recent member and make them owner.
                if (regionManager.hasMember(region) && !regionManager.hasOwner(region)) {

                    String member = regionManager.getRecentMember(region);

                    regionManager.makeOwner(region, member);

                    // Send a message to member that they are now the owner.
                    DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), member, "server",
                            ChatUtils.success("Transferred ownership of region %s to you due to the previous owner " + "leaving the region.", regionManager.getTag(region, member)),
                            true);
                    chatAPI.sendDirectMessage(directMessage);
                } else if (!regionManager.hasOwner(region) && !regionManager.hasMember(region)) {

                    // The region is has no owner and members, set the status to default.
                    regionManager.setDefault(region);
                }
            }
            case "join" -> {

                // Get the region.
                region = regionManager.getRegion(event[2]);

                // Add player to the region.
                // Create a copy of the coordinate id that the owner has.
                // The reason for a copy rather than using the same copy id is for if the user wants to set a new
                // location.
                // This then allows us to update the existing coordinate rather than create a new coordinate each
                // time this is done.
                int originalCoordinateID = regionManager.getCoordinateID(region, regionManager.getOwner(region));
                int coordinateID = coordinateAPI.copyCoordinate(originalCoordinateID);
                regionManager.joinRegion(region, uuid, coordinateID == -1 ? originalCoordinateID : coordinateID);

                // Send a message to the plot owner.
                DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), regionManager.getOwner(region), "server",
                        ChatUtils.success("%s has joined the region %s.", globalSQL.getString("SELECT name FROM player_data WHERE " + "uuid='" + uuid + "';"),
                                regionManager.getTag(region, regionManager.getOwner(region))), true);
                chatAPI.sendDirectMessage(directMessage);
            }
        }
    }
}
