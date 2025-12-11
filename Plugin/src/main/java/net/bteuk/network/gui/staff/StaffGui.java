package net.bteuk.network.gui.staff;

import net.bteuk.network.api.plotsystem.SubmittedPlot;
import net.bteuk.network.core.Constants;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.gui.regions.ReviewRegionRequests;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.regions.RegionUser;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class StaffGui extends NetworkRefreshableGui {

    private final NetworkUser user;
    private final Constants constants;

    public StaffGui(GuiProvider provider, NetworkUser user) {

        super(provider, 27, Component.text("Staff Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.constants = provider.constants();
    }

    protected void createGui() {

        if (constants.warpsEnabled()) {
            // Check if any location requests exist.
            // To make sure the string makes grammatical sense, we check if the number is 1, in this case we change 'are'
            // to 'is'.
            int lRequestCount = provider.globalSQL().getInt("SELECT COUNT(location) FROM " + "location_requests");
            Component lRequestString;
            if (lRequestCount == 1) {
                lRequestString = Utils.line("There is currently ").append(Component.text(1, NamedTextColor.GRAY)).append(Utils.line(" location request."));
            } else {
                lRequestString = Utils.line("There are currently ").append(Component.text(lRequestCount, NamedTextColor.GRAY)).append(Utils.line(" location requests."));
            }

            // Create item.
            setItem(25, Utils.createItem(Material.ENDER_CHEST, 1, Utils.title("Location Requests"), Utils.line("Opens a menu to view all location requests for navigation."),
                    lRequestString), (NetworkUser u) -> {

                // Check if the user has the relevant permissions.
                if (provider.globalSQL().getInt("SELECT COUNT(location) FROM location_requests") > 0) {
                    if (u.player.hasPermission("uknet.navigation.review")) {

                        // Open the LocationRequest gui.
                        this.delete();
                        u.staffGui = null;

                        u.staffGui = new LocationRequests(provider);
                        u.staffGui.open(u.player);
                    } else {
                        u.player.sendMessage(ChatUtils.error("You must be a reviewer to review location requests" + "."));
                    }
                } else {
                    u.player.sendMessage(ChatUtils.error("There are currently no location requests."));
                }
            });
        }

        if (constants.regionsEnabled()) {
        /*
        Click to open the menu to edit region details.

        Event team:
            Make any region open or public.

        Moderators:
            Remove people from regions or transfer ownership.
            Lock regions.

         */
            // If the player is in a region, show manage region, else show no region.
            Optional<RegionUser> optionalRegionUser = provider.regionManager().getUserByPlayer(user.player);
            if (optionalRegionUser.isPresent() && optionalRegionUser.get().hasTrackedRegion()) {
                RegionUser regionUser = optionalRegionUser.get();

                setItem(3, Utils.createItem(Material.ANVIL, 1, Utils.title("Manage Region " + regionUser.getTrackedRegion().regionName()),
                        Utils.line("Opens a menu to manage details of the region you are currently in.")), (NetworkUser u) ->

                {

                    // Check if the user has the relevant permissions.
                    if (u.player.hasPermission("uknet.regions.manage")) {

                        if (regionUser.hasTrackedRegion()) {
                            // Open manage region menu
                            this.delete();
                            u.staffGui = new ManageRegion(provider, u, regionUser.getTrackedRegion());
                            u.staffGui.open(u.player);
                        }
                    }

                    // Check if the user is in a region.

                    // Manage Region Menu.

                });
            } else {

                setItem(3, Utils.createItem(Material.STRUCTURE_VOID, 1, Utils.title("No Region"), Utils.line("You are currently not standing in a valid region."),
                        Utils.line("This is likely due to being in a lobby.")));
            }

            // Click to open menu to deal with region join requests.
            // Can only click on this if requests exist and player is a reviewer.
            // Check if any location requests exist.
            // To make sure the string makes grammatical sense we check if the number is 1, in this case we change 'are'
            // to 'is'.
            int rRequestCount = provider.regionSQL().getInt("SELECT COUNT(region) FROM region_requests WHERE " + "staff_accept=0");
            Component rRequestString;
            if (rRequestCount == 1) {
                rRequestString = Utils.line("There is currently ").append(Component.text(1, NamedTextColor.GRAY)).append(Utils.line(" region join request by Jr.Builders."));
            } else {
                rRequestString = Utils.line("There are currently ").append(Component.text(rRequestCount, NamedTextColor.GRAY))
                        .append(Utils.line(" region join requests by Jr.Builders."));
            }

            setItem(19, Utils.createItem(Material.CHEST_MINECART, 1, Utils.title("Review Region Requests"),
                    Utils.line("Opens a menu to review active region join requests by Jr.Builders."), rRequestString), (NetworkUser u) -> {

                if (provider.regionSQL().hasRow("SELECT region FROM region_requests WHERE " + "staff_accept=0;")) {
                    if (u.player.hasPermission("uknet.regions.request")) {

                        // Open region request menu.
                        this.delete();
                        u.staffGui = null;

                        u.staffGui = new ReviewRegionRequests(provider, true, u.player.getUniqueId().toString());
                        u.staffGui.open(u.player);
                    } else {
                        u.player.sendMessage(ChatUtils.error("You must be a reviewer to review region requests."));
                    }
                } else {
                    u.player.sendMessage(ChatUtils.error("There are currently no region requests."));
                }
            });
        }

        if (constants.plotSystemEnabled()) {
            // Click to review a plot.
            // Show the review plot button in gui.
            boolean isArchitect = user.hasPermission("group.architect");
            boolean isReviewer = user.hasPermission("group.reviewer");
            int reviewCount = provider.plotSQL().getReviewablePlotCount(user.player.getUniqueId().toString(), isArchitect, isReviewer);
            Component plotReviewMessage;

            if (reviewCount == 1) {
                plotReviewMessage = Utils.line("There is currently ").append(Component.text("1", NamedTextColor.GRAY)).append(Utils.line(" submitted plot."));
            } else {
                plotReviewMessage = Utils.line("There are currently ").append(Component.text(reviewCount, NamedTextColor.GRAY)).append(Utils.line(" submitted plots."));
            }

            setItem(21, Utils.createItem(Material.WRITABLE_BOOK, 1, Utils.title("Review Plot"), Utils.line("Click to review a submitted plot."), plotReviewMessage),
                    (NetworkUser u) -> {

                        // Get an arraylist of submitted plots.
                        // Order them by submitted time, so the oldest submissions are reviewed first.
                        List<SubmittedPlot> nPlots = provider.plotSQL().getReviewablePlots(u.player.getUniqueId().toString(), isArchitect, isReviewer);
                        nPlots.sort(Comparator.comparingLong(SubmittedPlot::submitTime));

                        // Check if there is a plot available to review
                        // that you are not already the owner or member of.
                        if (!nPlots.isEmpty()) {

                            int plotID = nPlots.getFirst().id();

                            // Get the server of the plot.
                            String server = provider.plotSQL().getString("SELECT server FROM " + "location_data WHERE name='" + provider.plotSQL()
                                    .getString("SELECT location FROM plot_data WHERE " + "id=" + plotID + ";") + "';");

                            // If they are not in the same server as the plot, teleport them to that server and start the
                            // reviewing process.
                            if (server.equals(constants.serverName())) {
                                u.player.closeInventory();
                                provider.eventAPI().createEvent(u.getUuid(), constants.serverName(), "review plot " + plotID);
                            } else {
                                // Player is not on the current server.
                                // Set the server join event.
                                provider.eventAPI().createJoinEvent(u.getUuid(), "review plot " + plotID);

                                // Teleport them to the server.
                                u.player.closeInventory();
                                provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), server);
                            }
                        } else {
                            u.player.sendMessage(ChatUtils.error("There are currently no submitted plots that you can " + "review.."));
                        }
                    });

            int verifyCount = provider.plotSQL().getVerifiablePlotCount(user.player.getUniqueId().toString(), isReviewer);
            Component plotVerifyMessage;

            if (verifyCount == 1) {
                plotVerifyMessage = Utils.line("There is currently ").append(Component.text("1", NamedTextColor.GRAY)).append(Utils.line(" plot awaiting verification."));
            } else {
                plotVerifyMessage = Utils.line("There are currently ").append(Component.text(verifyCount, NamedTextColor.GRAY)).append(Utils.line(" plots awaiting verification."));
            }
            setItem(23, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, Utils.title("Verify Plot"), Utils.line("Click to verify a reviewed plot."), plotVerifyMessage),
                    (NetworkUser u) ->

                    {

                        // Get an arraylist of reviewed plots.
                        // Order them by submitted time, so the oldest submissions are verified first.
                        List<Integer> nPlots = provider.plotSQL().getVerifiablePlots(u.player.getUniqueId().toString(), isReviewer);

                        // Check if there is a plot available to review,
                        // that you are not already the owner or member of.
                        if (!nPlots.isEmpty()) {

                            int plotID = nPlots.getFirst();

                            // Get server of plot.
                            String server = provider.plotSQL().getString("SELECT server FROM " + "location_data WHERE name='" + provider.plotSQL()
                                    .getString("SELECT location FROM plot_data WHERE " + "id=" + plotID + ";") + "';");

                            // If they are not in the same server as the plot teleport them to that server and start the
                            // reviewing process.
                            if (server.equals(constants.serverName())) {
                                u.player.closeInventory();
                                provider.eventAPI().createEvent(u.getUuid(), constants.serverName(), "verify plot " + plotID);
                            } else {
                                // Player is not on the current server.
                                // Set the server join event.
                                provider.eventAPI().createJoinEvent(u.getUuid(), "verify plot " + plotID);

                                // Teleport them to the server.
                                u.player.closeInventory();
                                provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), server);
                            }
                        } else {
                            u.player.sendMessage(ChatUtils.error("There are currently no submitted plots that you can review.."));
                        }
                    });
        }

        if (constants.moderationEnabled()) {

            // Click to open moderation menu.
            setItem(5, Utils.createItem(Material.REDSTONE_BLOCK, 1, Utils.title("Moderation Menu"), Utils.line("Opens the moderation menu to deal with wrongdoers.")),
                    (NetworkUser u) ->

                    {

                        // Check if the NetworkUser has any of the following permissions.
                        if (u.hasAnyPermission("uknet.ban", "uknet.mute", "uknet.kick")) {

                            this.delete();

                            u.staffGui = new ModerationGui(provider);
                            u.staffGui.open(u.player);
                        } else {

                            u.player.sendMessage(ChatUtils.error("You do not have permission to access the Moderation Menu."));
                            // Don't close the inventory as the player could have just miss-clicked, the chat should
                            // still be visible either way.
                            // Staff users should also be aware if they have this permission beforehand.

                        }
                    });
        }
    }
}