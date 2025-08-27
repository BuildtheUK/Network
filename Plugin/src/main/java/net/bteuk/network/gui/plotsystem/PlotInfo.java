package net.bteuk.network.gui.plotsystem;

import lombok.Setter;
import net.bteuk.network.Network;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.gui.InviteMembers;
import net.bteuk.network.gui.tutorials.RecommendedTutorialsGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.PlotValues;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.PlotStatus;
import net.bteuk.network.utils.enums.RegionType;
import net.bteuk.network.utils.enums.ServerType;
import net.bteuk.network.utils.enums.SubmittedStatus;
import net.bteuk.network.utils.plotsystem.ReviewFeedback;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.bteuk.network.utils.Constants.EARTH_WORLD;
import static net.bteuk.network.utils.Constants.SERVER_NAME;
import static net.bteuk.network.utils.Constants.SERVER_TYPE;

public class PlotInfo extends Gui {

    private final int plotID;
    private final NetworkUser user;

    private final PlotSQL plotSQL;
    private final GlobalSQL globalSQL;

    private String plot_owner;

    @Setter
    private AcceptedPlotMenu acceptedPlotMenu;

    public PlotInfo(NetworkUser user, int plotID) {

        // Create the menu.
        super(27, Component.text("Plot " + plotID, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.plotID = plotID;

        // Get plot sql.
        plotSQL = Network.getInstance().getPlotSQL();

        // Get global sql.
        globalSQL = Network.getInstance().getGlobalSQL();

        createGui();
    }

    public void createGui() {

        // Get the plot status.
        PlotStatus status = PlotStatus.fromDatabaseValue(plotSQL.getString("SELECT status FROM plot_data WHERE id=" + plotID + ";"));
        SubmittedStatus submittedStatus = null;
        if (status == null) {
            user.player.sendMessage(ChatUtils.error("This plot has an invalid status, can't open the info menu."));
            return;
        } else if (status == PlotStatus.SUBMITTED) {
            submittedStatus = SubmittedStatus.fromDatabaseValue(plotSQL.getString("SELECT status FROM plot_submission" + " WHERE plot_id=" + plotID + ";"));
        }
        // Get the plot owner.
        if (status == PlotStatus.CLAIMED || status == PlotStatus.SUBMITTED) {
            plot_owner = plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plotID + " AND is_owner=1;");
        } else if (status == PlotStatus.COMPLETED) {
            plot_owner = plotSQL.getString("SELECT uuid FROM plot_review WHERE plot_id=" + plotID + " AND accepted=1 AND " + "completed=1;");
        }
        // Determine the type of menu to create.
        PLOT_INFO_TYPE plotInfoType = determineMenuType(status, submittedStatus);
        if (plotInfoType == null || plotInfoType == PLOT_INFO_TYPE.DELETED) {
            user.player.sendMessage(ChatUtils.error("This plot not longer exists, can't open the info menu."));
            return;
        }

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the plot menu.")), (NetworkUser u) -> {

            // Switch back to plot menu, or accepted plot menu.
            if (status == PlotStatus.COMPLETED && acceptedPlotMenu != null) {
                this.deleteThis();
                acceptedPlotMenu.setPlotInfo(null);
                acceptedPlotMenu.open(u.player);
            } else {
                // Delete this gui.
                this.delete();
                u.mainGui = new PlotMenu(u);
                u.mainGui.open(u.player);
            }
        });

        // Plot Info
        setItem(4, Utils.createItem(Material.BOOK, 1, Utils.title("Plot " + plotID), createPlotInfo(status)));

        // Plot Teleport (Always in slot 24).
        setItem(24, Utils.createItem(Material.ENDER_PEARL, 1, Utils.title("Teleport to Plot"), Utils.line("Click to teleport to this plot.")), (NetworkUser u) -> {
            u.player.closeInventory();

            // Get the server of the plot.
            String server = plotSQL.getString(
                    "SELECT server FROM location_data WHERE name='" + plotSQL.getString("SELECT location FROM plot_data WHERE id=" + plotID + ";") + "';");

            // If the server is null it implies the location in the plotsystem was removed, teleport them to the location the plot should be in the Earth server.
            if (server == null) {
                teleportToPlotOutsidePlotsystem(u, plotID);
                return;
            }

            // If the plot is on the current server teleport them directly.
            // Else teleport them to the correct server and then teleport them to the plot.
            if (server.equals(SERVER_NAME)) {
                EventManager.createTeleportEvent(false, u.player.getUniqueId().toString(), "plotsystem", "teleport plot " + plotID, u.player.getLocation());
            } else {
                // Set the server join event.
                EventManager.createTeleportEvent(true, u.player.getUniqueId().toString(), "plotsystem", "teleport plot " + plotID, u.player.getLocation());

                // Teleport them to another server.
                SwitchServer.switchServer(u.player, server);
            }
        });

        // Plot in Google Maps (In slot 20 or 23 depending on the situation).
        setItem(getSlotForGoogleMapsLink(plotInfoType),
                Utils.createItem(Material.ENDER_EYE, 1, Utils.title("View plot in Google Maps"), Utils.line("Click to be linked to the plot in Google Maps.")), (NetworkUser u) -> {
                    u.player.closeInventory();

                    // Get corners of the plot.
                    int[][] corners = plotSQL.getPlotCorners(plotID);
                    int sumX = 0;
                    int sumZ = 0;

                    // Find the centre.
                    for (int[] corner : corners) {

                        sumX += corner[0];
                        sumZ += corner[1];
                    }
                    double x = sumX / (double) corners.length;
                    double z = sumZ / (double) corners.length;

                    // Convert to irl coordinates.
                    try {

                        final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
                        double[] coords = bteGeneratorSettings.projection().toGeo(x, z);

                        // Generate link to google maps.
                        Component message = Component.text("Click here to open the plot in Google Maps", NamedTextColor.GREEN);
                        message = message.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL,
                                "https://www" + ".google.com/maps/@?api=1&map_action=map&basemap=satellite&zoom=21&center=" + coords[1] + "," + coords[0]));

                        u.player.sendMessage(message);
                        u.player.closeInventory();
                    } catch (OutOfProjectionBoundsException e) {
                        u.player.sendMessage(ChatUtils.error("Can't find the location of this plot."));
                        u.player.closeInventory();
                    }
                });

        // Enable/disable outlines for the plot. (Slot 18 if owner or member)
        if (plotInfoType == PLOT_INFO_TYPE.CLAIMED_OWNER || plotInfoType == PLOT_INFO_TYPE.CLAIMED_MEMBER) {
            setItem(18, Utils.createItem(Material.ORANGE_STAINED_GLASS, 1, Utils.title("Toggle Outlines"), Utils.line("Enable/disable the outlines"), Utils.line("for this plot."),
                    Utils.line("Rejoining the server"), Utils.line("will reset this to enabled.")), (NetworkUser u) -> {
                EventManager.createEvent(u.player.getUniqueId().toString(), "plotsystem", SERVER_NAME, "outlines toggle " + plotID);
                u.player.closeInventory();
            });
        }

        // For the plot owner, add the manage and invite members options. (Slot 20 and 21)
        // As well as the submit/retract button. (Slot 2)
        // If the plot is not under review allow it to be removed. (Slot 6)
        if (plotInfoType == PLOT_INFO_TYPE.CLAIMED_OWNER) {
            setItem(20, Utils.createItem(Material.PLAYER_HEAD, 1, Utils.title("Plot Members"), Utils.line("Manage the members of your plot.")), (NetworkUser u) -> {

                // Delete this gui.
                this.delete();
                u.mainGui = null;

                // Switch back to plot menu.
                u.mainGui = new PlotsystemMembers(plotID, RegionType.PLOT);
                u.mainGui.open(u.player);
            });

            setItem(19, Utils.createItem(Material.OAK_BOAT, 1, Utils.title("Invite Members"), Utils.line("Invite a new member to your plot."),
                    Utils.line("You can only invite online users.")), (NetworkUser u) -> {

                // Delete this gui.
                this.delete();
                u.mainGui = null;

                // Switch back to plot menu.
                u.mainGui = new InviteMembers(plotID, RegionType.PLOT);
                u.mainGui.open(u.player);
            });

            if (status == PlotStatus.CLAIMED) {
                setItem(2, Utils.createItem(Material.LIGHT_BLUE_CONCRETE, 1, Utils.title("Submit Plot"), Utils.line("Submit your plot to be reviewed."),
                        Utils.line("Reviewing may take over 24 hours.")), (NetworkUser u) -> {

                    u.player.closeInventory();

                    // Add server event to submit plot.
                    globalSQL.update("INSERT INTO server_events(uuid,type,server,event) VALUES('" + u.player.getUniqueId() + "','plotsystem','" + plotSQL.getString(
                            "SELECT server FROM location_data WHERE name='" + plotSQL.getString(
                                    "SELECT location FROM plot_data WHERE id=" + plotID + ";") + "';") + "','submit plot " + plotID + "');");
                });
            }

            // The plot can only be retracted if it is not yet under review.
            if (status == PlotStatus.SUBMITTED && submittedStatus == SubmittedStatus.SUBMITTED) {
                setItem(2, Utils.createItem(Material.ORANGE_CONCRETE, 1, Utils.title("Retract Submission"), Utils.line("Your plot will no longer be submitted.")), (NetworkUser u) -> {

                    u.player.closeInventory();

                    // Add server event to retract plot submission.
                    globalSQL.update("INSERT INTO server_events(uuid,type,server,event) VALUES('" + u.player.getUniqueId() + "','plotsystem','" + plotSQL.getString(
                            "SELECT server FROM location_data WHERE name='" + plotSQL.getString(
                                    "SELECT location FROM plot_data WHERE id=" + plotID + ";") + "';") + "','retract plot " + plotID + "');");
                });
            }

            // The plot can only be deleted if it is not yet submitted.
            if (status != PlotStatus.SUBMITTED) {
                setItem(6, Utils.createItem(Material.RED_CONCRETE, 1, Utils.title("Delete Plot"), Utils.line("Delete the plot and all its contents.")), (NetworkUser u) -> {

                    // Delete this gui.
                    this.delete();
                    u.mainGui = null;

                    // Switch back to plot menu.
                    u.mainGui = new DeleteConfirm(plotID, RegionType.PLOT);
                    u.mainGui.open(u.player);
                });
            }
        }

        // Members have the option to leave the plot (Slot 20)
        if (plotInfoType == PLOT_INFO_TYPE.CLAIMED_MEMBER) {
            setItem(20, Utils.createItem(Material.RED_CONCRETE, 1, Utils.title("Leave Plot"), Utils.line("You will not be able to build in the plot once you leave.")), (NetworkUser u) -> {

                // Delete this gui.
                this.delete();
                u.mainGui = null;

                // Switch back to plot menu.
                Bukkit.getScheduler().scheduleSyncDelayedTask(Network.getInstance(), () -> {
                    u.mainGui = new PlotMenu(u);
                    u.mainGui.open(u.player);
                }, 20L);

                // Add server event to leave plot.
                globalSQL.update("INSERT INTO server_events(uuid,type,server,event) VALUES('" + u.player.getUniqueId() + "','plotsystem','" + plotSQL.getString(
                        "SELECT server FROM location_data WHERE name='" + plotSQL.getString(
                                "SELECT location FROM plot_data WHERE id=" + plotID + ";") + "';") + "','leave plot " + plotID + "');");
            });
        }

        // If this plot has feedback, add feedback for the plot owner and members (Slot 22)
        // As well as for reviewers (Slot 22 while submitted, reviewed or reviewing)
        if ((plotInfoType == PLOT_INFO_TYPE.CLAIMED_OWNER || plotInfoType == PLOT_INFO_TYPE.CLAIMED_MEMBER || plotInfoType == PLOT_INFO_TYPE.REVIEWING_REVIEWER || plotInfoType == PLOT_INFO_TYPE.SUBMITTED_REVIEWER || plotInfoType == PLOT_INFO_TYPE.REVIEWED_REVIEWER || plotInfoType == PLOT_INFO_TYPE.VERIFYING_REVIEWER) && plotSQL.hasRow(
                "SELECT 1 FROM plot_review WHERE plot_id=" + plotID + " AND uuid='" + plot_owner + "' AND accepted=0 AND completed=1;")) {
            setItem(getFeedbackSlot(plotInfoType), Utils.createItem(Material.WRITABLE_BOOK, 1, Utils.title("Plot Feedback"), Utils.line("Click to show feedback for this plot.")),
                    (NetworkUser u) -> {

                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch back to plot menu.
                        u.mainGui = new DeniedPlotFeedback(plotID);
                        u.mainGui.open(u.player);
                    });
            // If the plot is accepted and has feedback show for the owner (Slot 21)
        } else if (plotInfoType == PLOT_INFO_TYPE.ACCEPTED_OWNER && plotSQL.hasRow(
                "SELECT 1 FROM " + "plot_category_feedback WHERE review_id=( SELECT id FROM plot_review WHERE plot_id=" + plotID + " AND " + "accepted=1 AND completed=1 );")) {
            setItem(getFeedbackSlot(plotInfoType), Utils.createItem(Material.WRITABLE_BOOK, 1, Utils.title("Plot Feedback"), Utils.line("Click to show feedback for this plot.")),
                    (NetworkUser u) -> {
                        int reviewId = plotSQL.getInt(
                                "SELECT id FROM plot_review WHERE uuid='" + u.getUuid() + "' " + "AND plot_id=" + plotID + " AND accepted=1 AND completed=1;");

                        // Open the feedback book.
                        u.player.openBook(ReviewFeedback.createFeedbackBook(reviewId));
                    });
        }

        // Tutorial recommendations
        switch (plotInfoType) {
            case CLAIMED_OWNER, CLAIMED_MEMBER, ACCEPTED_OWNER -> {
                setItem(getRecommendationsSlot(plotInfoType), Utils.createItem(Material.LECTERN, 1, Utils.title("Tutorial Recommendations"),
                        Utils.line("Click to see your"), Utils.line("recommended tutorials")), (NetworkUser u) -> {
                    user.mainGui = new RecommendedTutorialsGui(this, plotID, user, plot_owner, false);
                    user.mainGui.open(user);
                });
            }
            case SUBMITTED_REVIEWER, REVIEWED_REVIEWER, REVIEWING_REVIEWER, VERIFYING_REVIEWER -> {
                setItem(getRecommendationsSlot(plotInfoType), Utils.createItem(Material.LECTERN, 1, Utils.title("Tutorial Recommendations"),
                        Utils.line("Click to see the"), Utils.line("tutorial recommendations"), Utils.line("and add more")), (NetworkUser u) -> {
                    user.mainGui = new RecommendedTutorialsGui(this, plotID, user, plot_owner, true);
                    user.mainGui.open(user);
                });
            }
            case CLAIMED, ACCEPTED -> {
                // Reviewers can always add recommendations to claimed and accepted plots
                // Architects can always add recommendations to claimed plots
                if (user.hasPermission("group.reviewer") || (user.hasPermission("group.architect") && plotInfoType.equals(PLOT_INFO_TYPE.CLAIMED))) {
                    setItem(getRecommendationsSlot(plotInfoType), Utils.createItem(Material.LECTERN, 1, Utils.title("Tutorial Recommendations"),
                            Utils.line("Click to see the"), Utils.line("tutorial recommendations"), Utils.line("and add more")), (NetworkUser u) -> {
                        user.mainGui = new RecommendedTutorialsGui(this, plotID, user, plot_owner, true);
                        user.mainGui.open(user);
                    });
                }
            }
        }

        // If the plot is submitted add the start review option for reviewers. (Slot 20)
        if (plotInfoType == PLOT_INFO_TYPE.SUBMITTED_REVIEWER) {
            setItem(20, Utils.createItem(Material.EMERALD, 1, Utils.title("Review Plot"), Utils.line("Click to start reviewing this plot.")), (NetworkUser u) -> {
                // If you are not owner or member of the plot, start the review.
                if (canReviewPlot()) {
                    // Get server of plot.
                    String server = Network.getInstance().getPlotSQL().getString("SELECT server FROM " + "location_data WHERE name='" + Network.getInstance().getPlotSQL()
                            .getString("SELECT location FROM plot_data " + "WHERE id=" + plotID + ";") + "';");

                    // If they are not in the same server as the plot teleport them to that server and start
                    // the reviewing process.
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
                    user.player.sendMessage(ChatUtils.error("You are not allowed to review this plot."));
                }
            });
            // If the plot has been reviewed and must be verified add the start verifying option for reviewers. (Slot
            // 20)
        } else if (plotInfoType == PLOT_INFO_TYPE.REVIEWED_REVIEWER) {
            setItem(20, Utils.createItem(Material.SPYGLASS, 1, Utils.title("Verify Plot"), Utils.line("Click to start verifying this plot.")), (NetworkUser u) -> {
                if (canVerifyPlot()) {
                    // Get server of plot.
                    String server = Network.getInstance().getPlotSQL().getString("SELECT server FROM " + "location_data WHERE name='" + Network.getInstance().getPlotSQL()
                            .getString("SELECT location FROM plot_data " + "WHERE id=" + plotID + ";") + "';");

                    // If they are not in the same server as the plot teleport them to that server and start
                    // the reviewing process.
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
                    user.player.sendMessage(ChatUtils.error("You are not allowed to verify this plot."));
                }
            });
        }
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }

    @Override
    public void delete() {
        if (acceptedPlotMenu != null) {
            acceptedPlotMenu.delete();
        } else {
            deleteThis();
        }
    }

    public void deleteThis() {
        super.delete();
    }

    private PLOT_INFO_TYPE determineMenuType(PlotStatus status, SubmittedStatus submittedStatus) {
        return switch (status) {
            case UNCLAIMED -> PLOT_INFO_TYPE.UNCLAIMED;
            case CLAIMED -> claimedType();
            case SUBMITTED -> determineMenuTypeSubmitted(submittedStatus);
            case COMPLETED -> {
                if (Objects.equals(plot_owner, user.player.getUniqueId().toString())) {
                    yield PLOT_INFO_TYPE.ACCEPTED_OWNER;
                } else {
                    yield PLOT_INFO_TYPE.ACCEPTED;
                }
            }
            case DELETED -> PLOT_INFO_TYPE.DELETED;
        };
    }

    private PLOT_INFO_TYPE determineMenuTypeSubmitted(SubmittedStatus submittedStatus) {
        return switch (submittedStatus) {
            case SUBMITTED -> {
                if (canReviewPlot()) {
                    yield PLOT_INFO_TYPE.SUBMITTED_REVIEWER;
                } else {
                    yield claimedType();
                }
            }
            case UNDER_REVIEW -> {
                if (canReviewPlot()) {
                    yield PLOT_INFO_TYPE.REVIEWING_REVIEWER;
                } else {
                    yield claimedType();
                }
            }
            case AWAITING_VERIFICATION -> {
                if (canVerifyPlot()) {
                    yield PLOT_INFO_TYPE.REVIEWED_REVIEWER;
                } else {
                    yield claimedType();
                }
            }
            case UNDER_VERIFICATION -> {
                if (canVerifyPlot()) {
                    yield PLOT_INFO_TYPE.VERIFYING_REVIEWER;
                } else {
                    yield claimedType();
                }
            }
        };
    }

    private boolean canReviewPlot() {
        boolean isArchitect = user.hasPermission("group.architect");
        boolean isReviewer = user.hasPermission("group.reviewer");
        return plotSQL.canReviewPlot(plotID, user.getUuid(), isArchitect, isReviewer);
    }

    private boolean canVerifyPlot() {
        boolean isReviewer = user.hasPermission("group.reviewer");
        return plotSQL.canVerifyPlot(plotID, user.getUuid(), isReviewer);
    }

    private PLOT_INFO_TYPE claimedType() {
        if (Objects.equals(plot_owner, user.player.getUniqueId().toString())) {
            return PLOT_INFO_TYPE.CLAIMED_OWNER;
        } else if (plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plotID + " AND uuid='" + user.player.getUniqueId() + "' AND" + " is_owner=0;")) {
            return PLOT_INFO_TYPE.CLAIMED_MEMBER;
        } else {
            return PLOT_INFO_TYPE.CLAIMED;
        }
    }

    private Component[] createPlotInfo(PlotStatus status) {
        List<Component> info = new ArrayList<>();
        if (status == PlotStatus.CLAIMED || status == PlotStatus.SUBMITTED) {
            info.add(Utils.line("Plot Owner: ").append(Component.text(globalSQL.getString(
                            "SELECT name FROM player_data WHERE uuid='" + plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plotID + " AND is_owner=1;") + "';"),
                    NamedTextColor.GRAY)));
            info.add(Utils.line("Plot Members: ")
                    .append(Component.text(plotSQL.getInt("SELECT COUNT(uuid) FROM plot_members WHERE id=" + plotID + " AND is_owner=0;"), NamedTextColor.GRAY)));
        } else if (status == PlotStatus.COMPLETED) {
            info.add(Utils.line("Completed by: ").append(Component.text(globalSQL.getString("SELECT name FROM " + "player_data WHERE uuid='" + plotSQL.getString(
                    "SELECT uuid FROM plot_review WHERE plot_id=" + plotID + " AND accepted=1 AND" + " completed=1;") + "';"), NamedTextColor.GRAY)));
            info.add(Utils.line("Accepted by: ").append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + plotSQL.getString(
                    "SELECT reviewer FROM plot_review WHERE plot_id=" + plotID + " AND " + "accepted=1 AND completed=1;") + "';"), NamedTextColor.GRAY)));
        } else if (status == PlotStatus.UNCLAIMED) {
            info.add(Utils.line("This plot is unclaimed!"));
        }

        // Add size and difficulty stats.
        info.add(Utils.line("Difficulty: ")
                .append(Component.text(PlotValues.difficultyName(plotSQL.getInt("SELECT difficulty FROM plot_data " + "WHERE id=" + plotID + ";")), NamedTextColor.GRAY)));
        info.add(Utils.line("Size: ").append(Component.text(PlotValues.sizeName(plotSQL.getInt("SELECT size FROM plot_data WHERE id=" + plotID + ";")), NamedTextColor.GRAY)));

        // If accepted, add a disclaimer that the actual plot may have changed since it was accepted.
        if (status == PlotStatus.COMPLETED) {
            info.add(Component.text("Disclaimer: ", NamedTextColor.WHITE, TextDecoration.BOLD).append(Utils.line("the content of")));
            info.add(Utils.line("the plot may have changed"));
            info.add(Utils.line("since it was completed!"));
        }
        return info.toArray(Component[]::new);
    }

    private int getSlotForGoogleMapsLink(PLOT_INFO_TYPE plotInfoType) {
        if (plotInfoType == PLOT_INFO_TYPE.CLAIMED_OWNER || plotInfoType == PLOT_INFO_TYPE.CLAIMED_MEMBER || plotInfoType == PLOT_INFO_TYPE.SUBMITTED_REVIEWER || plotInfoType == PLOT_INFO_TYPE.REVIEWED_REVIEWER || plotInfoType == PLOT_INFO_TYPE.ACCEPTED_OWNER) {
            return 23;
        } else {
            return 20;
        }
    }

    private int getFeedbackSlot(PLOT_INFO_TYPE plotInfoType) {
        if (plotInfoType == PLOT_INFO_TYPE.CLAIMED_OWNER || plotInfoType == PLOT_INFO_TYPE.CLAIMED_MEMBER || plotInfoType == PLOT_INFO_TYPE.SUBMITTED_REVIEWER || plotInfoType == PLOT_INFO_TYPE.REVIEWED_REVIEWER || plotInfoType == PLOT_INFO_TYPE.REVIEWING_REVIEWER || plotInfoType == PLOT_INFO_TYPE.VERIFYING_REVIEWER) {
            return 22;
        } else if (plotInfoType == PLOT_INFO_TYPE.ACCEPTED_OWNER) {
            return 21;
        } else {
            return -1;
        }
    }

    private int getRecommendationsSlot(PLOT_INFO_TYPE plotInfoType) {
        return switch (plotInfoType) {
            case CLAIMED_OWNER, CLAIMED_MEMBER, SUBMITTED_REVIEWER, REVIEWING_REVIEWER, VERIFYING_REVIEWER, REVIEWED_REVIEWER -> 21;
            case CLAIMED, ACCEPTED -> 22;
            case ACCEPTED_OWNER -> 20;
            default -> -1;
        };
    }

    private void teleportToPlotOutsidePlotsystem(NetworkUser user, int plotID) {
        // Get corners of the plot.
        int[][] corners = plotSQL.getPlotCorners(plotID);
        int sumX = 0;
        int sumZ = 0;

        // Find the centre.
        for (int[] corner : corners) {

            sumX += corner[0];
            sumZ += corner[1];
        }
        double x = sumX / (double) corners.length;
        double z = sumZ / (double) corners.length;

        // Teleport to the location on the Earth server.
        Component teleportMessage = ChatUtils.success("Teleported to accepted plot %s", String.valueOf(plotID));

        boolean switchServer = SERVER_TYPE != ServerType.EARTH;

        EventManager.createTeleportEvent(switchServer, user.player.getUniqueId().toString(), "network",
                "teleport " + EARTH_WORLD + " " + x + " " + z + " "
                        + user.player.getLocation().getYaw() + " " + user.player.getLocation().getPitch(), PlainTextComponentSerializer.plainText().serialize(teleportMessage),
                user.player.getLocation());

        // Switch to Earth server is necessary.
        if (switchServer) {
            user.player.closeInventory();
            SwitchServer.switchServer(user.player, Network.getInstance().getGlobalSQL().getString("SELECT name FROM server_data WHERE type='EARTH';"));
        }
    }

    private enum PLOT_INFO_TYPE {
        CLAIMED_OWNER,
        CLAIMED_MEMBER,
        CLAIMED,

        SUBMITTED_REVIEWER,

        REVIEWING_REVIEWER,

        REVIEWED_REVIEWER,

        VERIFYING_REVIEWER,

        ACCEPTED_OWNER,
        ACCEPTED,

        UNCLAIMED,
        DELETED
    }
}

