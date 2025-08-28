package net.bteuk.network.gui.staff;

import net.bteuk.network.Network;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.gui.regions.ReviewRegionRequests;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.plotsystem.SubmittedPlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.List;

import static net.bteuk.network.utils.Constants.SERVER_NAME;

public class StaffGui extends Gui {

    private final NetworkUser user;

    public StaffGui(NetworkUser user) {

        super(27, Component.text("Staff Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;

        createGui();
    }

    private void createGui() {

        // Check if any location requests exist.
        // To make sure the string makes grammatical sense we check if the number is 1, in this case we change 'are'
        // to 'is'.
        int lRequestCount = Network.getInstance().getGlobalSQL().getInt("SELECT COUNT(location) FROM " +
                "location_requests");
        Component lRequestString;
        if (lRequestCount == 1) {
            lRequestString = Utils.line("There is currently ")
                    .append(Component.text(1, NamedTextColor.GRAY))
                    .append(Utils.line(" location request."));
        } else {
            lRequestString = Utils.line("There are currently ")
                    .append(Component.text(lRequestCount, NamedTextColor.GRAY))
                    .append(Utils.line(" location requests."));
        }

        // Create item.
        setItem(25, Utils.createItem(Material.ENDER_CHEST, 1,
                        Utils.title("Location Requests"),
                        Utils.line("Opens a menu to view all location requests for navigation."),
                        lRequestString),
                (NetworkUser u) -> {

                    // Check if the user has the relevant permissions.
                    if (Network.getInstance().getGlobalSQL()
                            .getInt("SELECT COUNT(location) FROM location_requests") > 0) {
                        if (u.player.hasPermission("uknet.navigation.review")) {

                            // Open the LocationRequest gui.
                            this.delete();
                            u.staffGui = null;

                            u.staffGui = new LocationRequests();
                            u.staffGui.open(u.player);
                        } else {
                            u.player.sendMessage(ChatUtils.error("You must be a reviewer to review location requests" +
                                    "."));
                        }
                    } else {
                        u.player.sendMessage(ChatUtils.error("There are currently no location requests."));
                    }
                });

        /*
        Click to open menu to edit region details.

        Event team:
            Make any region open or public.

        Moderators:
            Remove people from regions, or transfer ownership.
            Lock regions.

         */
        // If player is in a region show manage region, else show no region.
        if (user.inRegion) {

            setItem(3, Utils.createItem(Material.ANVIL, 1,
                            Utils.title("Manage Region " + user.region.regionName()),
                            Utils.line("Opens a menu to manage details of the region you are currently in.")),
                    (NetworkUser u) ->

                    {

                        // Check if the user has the relevant permissions.
                        if (u.player.hasPermission("uknet.regions.manage")) {

                            if (u.inRegion) {
                                // Open manage region menu
                                this.delete();
                                u.staffGui = new ManageRegion(u, u.region);
                                u.staffGui.open(u.player);
                            }
                        }

                        // Check if the user is in a region.

                        // Manage Region Menu.

                    });
        } else {

            setItem(3, Utils.createItem(Material.STRUCTURE_VOID, 1,
                    Utils.title("No Region"),
                    Utils.line("You are currently not standing in a valid region."),
                    Utils.line("This is likely due to being in a lobby.")));
        }

        // Click to open menu to deal with region join requests.
        // Can only click on this if requests exist and player is a reviewer.
        // Check if any location requests exist.
        // To make sure the string makes grammatical sense we check if the number is 1, in this case we change 'are'
        // to 'is'.
        int rRequestCount = Network.getInstance().regionSQL.getInt("SELECT COUNT(region) FROM region_requests WHERE " +
                "staff_accept=0");
        Component rRequestString;
        if (rRequestCount == 1) {
            rRequestString = Utils.line("There is currently ")
                    .append(Component.text(1, NamedTextColor.GRAY))
                    .append(Utils.line(" region join request by Jr.Builders."));
        } else {
            rRequestString = Utils.line("There are currently ")
                    .append(Component.text(rRequestCount, NamedTextColor.GRAY))
                    .append(Utils.line(" region join requests by Jr.Builders."));
        }

        setItem(19, Utils.createItem(Material.CHEST_MINECART, 1,
                        Utils.title("Review Region Requests"),
                        Utils.line("Opens a menu to review active region join requests by Jr.Builders."),
                        rRequestString),
                (NetworkUser u) -> {

                    if (Network.getInstance().regionSQL.hasRow("SELECT region FROM region_requests WHERE " +
                            "staff_accept=0;")) {
                        if (u.player.hasPermission("uknet.regions.request")) {

                            // Open region request menu.
                            this.delete();
                            u.staffGui = null;

                            u.staffGui = new ReviewRegionRequests(true, u.player.getUniqueId().toString());
                            u.staffGui.open(u.player);
                        } else {
                            u.player.sendMessage(ChatUtils.error("You must be a reviewer to review region requests."));
                        }
                    } else {
                        u.player.sendMessage(ChatUtils.error("There are currently no region requests."));
                    }
                });

        // Click to review plot.
        // Show review plot button in gui.
        boolean isArchitect = user.hasPermission("group.architect");
        boolean isReviewer = user.hasPermission("group.reviewer");
        int reviewCount =
                Network.getInstance().getPlotSQL().getReviewablePlotCount(user.player.getUniqueId().toString(),
                        isArchitect, isReviewer);
        Component plotReviewMessage;

        if (reviewCount == 1) {
            plotReviewMessage = Utils.line("There is currently ")
                    .append(Component.text("1", NamedTextColor.GRAY))
                    .append(Utils.line(" submitted plot."));
        } else {
            plotReviewMessage = Utils.line("There are currently ")
                    .append(Component.text(reviewCount, NamedTextColor.GRAY))
                    .append(Utils.line(" submitted plots."));
        }

        setItem(21, Utils.createItem(Material.WRITABLE_BOOK, 1,
                        Utils.title("Review Plot"),
                        Utils.line("Click to review a submitted plot."),
                        plotReviewMessage),
                (NetworkUser u) -> {

                    // Get arraylist of submitted plots.
                    // Order them by submit time, so the oldest submissions are reviewed first.
                    List<SubmittedPlot> nPlots = Network.getInstance().getPlotSQL().getReviewablePlots(u.player.getUniqueId().toString(), isArchitect, isReviewer);
                    nPlots.sort(Comparator.comparingLong(SubmittedPlot::submitTime));

                    // Check if there is a plot available to review,
                    // that you are not already the owner or member of.
                    if (!nPlots.isEmpty()) {

                        int plotID = nPlots.getFirst().id();

                        // Get server of plot.
                        String server = Network.getInstance().getPlotSQL().getString("SELECT server FROM " +
                                "location_data WHERE name='" +
                                Network.getInstance().getPlotSQL().getString("SELECT location FROM plot_data WHERE " +
                                        "id=" + plotID + ";") + "';");

                        // If they are not in the same server as the plot teleport them to that server and start the
                        // reviewing process.
                        if (server.equals(SERVER_NAME)) {
                            u.player.closeInventory();
                            EventManager.createEvent(u.getUuid(), "plotsystem", SERVER_NAME, "review plot " + plotID);
                        } else {
                            // Player is not on the current server.
                            // Set the server join event.
                            EventManager.createJoinEvent(u.getUuid(), "plotsystem", "review plot " + plotID);

                            // Teleport them to the server.
                            u.player.closeInventory();
                            SwitchServer.switchServer(u.player, server);
                        }
                    } else {
                        u.player.sendMessage(ChatUtils.error("There are currently no submitted plots that you can " +
                                "review.."));
                    }
                });

        int verifyCount =
                Network.getInstance().getPlotSQL().getVerifiablePlotCount(user.player.getUniqueId().toString(), isReviewer);
        Component plotVerifyMessage;

        if (verifyCount == 1) {
            plotVerifyMessage = Utils.line("There is currently ")
                    .append(Component.text("1", NamedTextColor.GRAY))
                    .append(Utils.line(" plot awaiting verification."));
        } else {
            plotVerifyMessage = Utils.line("There are currently ")
                    .append(Component.text(verifyCount, NamedTextColor.GRAY))
                    .append(Utils.line(" plots awaiting verification."));
        }
        setItem(23, Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                        Utils.title("Verify Plot"),
                        Utils.line("Click to verify a reviewed plot."),
                        plotVerifyMessage),
                (NetworkUser u) ->

                {

                    // Get arraylist of reviewed plots.
                    // Order them by submit time, so the oldest submissions are verified first.
                    List<Integer> nPlots =
                            Network.getInstance().getPlotSQL().getVerifiablePlots(u.player.getUniqueId().toString(),
                                    isReviewer);

                    // Check if there is a plot available to review,
                    // that you are not already the owner or member of.
                    if (!nPlots.isEmpty()) {

                        int plotID = nPlots.getFirst();

                        // Get server of plot.
                        String server = Network.getInstance().getPlotSQL().getString("SELECT server FROM " +
                                "location_data WHERE name='" +
                                Network.getInstance().getPlotSQL().getString("SELECT location FROM plot_data WHERE " +
                                        "id=" + plotID + ";") + "';");

                        // If they are not in the same server as the plot teleport them to that server and start the
                        // reviewing process.
                        if (server.equals(SERVER_NAME)) {
                            u.player.closeInventory();
                            EventManager.createEvent(u.getUuid(), "plotsystem", SERVER_NAME, "verify plot " + plotID);
                        } else {
                            // Player is not on the current server.
                            // Set the server join event.
                            EventManager.createJoinEvent(u.getUuid(), "plotsystem", "verify plot " + plotID);

                            // Teleport them to the server.
                            u.player.closeInventory();
                            SwitchServer.switchServer(u.player, server);
                        }
                    } else {
                        u.player.sendMessage(ChatUtils.error("There are currently no submitted plots that you can review.."));
                    }
                });

        // Click to open moderation menu.
        setItem(5, Utils.createItem(Material.REDSTONE_BLOCK, 1,
                        Utils.title("Moderation Menu"),
                        Utils.line("Opens the moderation menu to deal with wrongdoers.")),
                (NetworkUser u) ->

                {

                    // Check if the NetworkUser has any of the following permissions.
                    if (u.hasAnyPermission("uknet.ban", "uknet.mute", "uknet.kick")) {

                        this.delete();

                        u.staffGui = new ModerationGui();
                        u.staffGui.open(u.player);
                    } else {

                        u.player.sendMessage(ChatUtils.error("You do not have permission to access the Moderation Menu."));
                        // Don't close the inventory as the player could have just miss-clicked, the chat should
                        // still be visible either way.
                        // Staff users should also be aware if they have this permission beforehand.

                    }
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }
}