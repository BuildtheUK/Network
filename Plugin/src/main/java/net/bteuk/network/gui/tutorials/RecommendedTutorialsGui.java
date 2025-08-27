package net.bteuk.network.gui.tutorials;

import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.TutorialRecommendation;
import net.bteuk.network.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.EventType;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.utils.DBConnection;

import java.util.UUID;

import static net.bteuk.network.utils.Constants.LOGGER;

public class RecommendedTutorialsGui extends Gui {
    private final Gui parentGui;

    private final int iPlotID;

    private final NetworkUser user;

    private final UUID ownerUUID;

    private TutorialRecommendation[] tutorialRecommendations;

    private final boolean bStaffView;

    /**
     * The number of pages in this menu
     */
    private int iPages;

    /**
     * The current page that the player is on
     */
    private int iPage;

    public RecommendedTutorialsGui(Gui parentGui, int iPlotID, NetworkUser user, String plotOwner, boolean bStaffView) {
        super(54, Utils.title("Recommended Tutorials"));
        this.parentGui = parentGui;
        this.iPlotID = iPlotID;
        this.user = user;
        if (plotOwner != null)
            this.ownerUUID = UUID.fromString(plotOwner);
        else
            this.ownerUUID = null;
        this.bStaffView = bStaffView;

        // Fetches the tutorial recommendations for this plot
        tutorialRecommendations = Network.getInstance().getPlotSQL().fetchTutorialRecommendationsForPlot(LOGGER, iPlotID);

        this.iPages = ((tutorialRecommendations.length - 1) / 45) + 1;
        this.iPage = 1;

        addItems();
    }

    private void addItems() {
        // Back button
        setItem(53, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        ChatUtils.title("Return")),

                (NetworkUser u) -> {
                    // Go back to the review gui.
                    u.player.closeInventory();
                    parentGui.open(u.player);
                }
        );

        // Indicates that there are no unfinished lessons
        if (tutorialRecommendations.length == 0) {
            ItemStack noRecommendations = Utils.createItem(Material.BARRIER, 1,
                    Utils.title("There are no tutorial recommendations"));
            setItem(5 - 1, noRecommendations);
        }

        // Page back
        if (iPage > 1) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    Utils.title("Page back"));
            super.setItem(47, pageBack, new guiAction() {
                @Override
                public void click(NetworkUser u) {
                    iPage--;
                    refresh();
                    user.mainGui = RecommendedTutorialsGui.this;
                    user.mainGui.open(user);
                }
            });
        }

        // Page forwards
        if (iPage < iPages) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44",
                    Material.ACACIA_BOAT, 1,
                    Utils.title("Page forwards"));
            super.setItem(51, pageBack, new guiAction() {
                @Override
                public void click(NetworkUser u) {
                    iPage++;
                    refresh();
                    user.mainGui = RecommendedTutorialsGui.this;
                    user.mainGui.open(user);
                }
            });
        }

        // Adds the tutorials
        int iStart = (iPage - 1) * 9;
        int iMax = Math.min((iPage + 4) * 9, tutorialRecommendations.length);
        int i;
        for (i = iStart; i < iMax; i++) {
            teachingtutorials.tutorialobjects.TutorialRecommendation tutorialRecommendation = tutorialRecommendations[i].getLinkedTutorialRecommendation();
            ItemStack recommendationIcon;

            // Display different icons depending on whether it is completed or not
            if (tutorialRecommendation.isCompleted()) {
                if (bStaffView)
                    recommendationIcon = teachingtutorials.utils.Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                            Utils.title(tutorialRecommendation.getTutorialName(Network.getInstance().getTutorialsDBConnection(), LOGGER)),
                            Utils.line("Recommended by " + tutorialRecommendation.getRecommenderName()),
                            Utils.line("Completed"));
                else
                    recommendationIcon = teachingtutorials.utils.Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                            Utils.title(tutorialRecommendation.getTutorialName(Network.getInstance().getTutorialsDBConnection(), LOGGER)),
                            Utils.line("Recommended by " + tutorialRecommendation.getRecommenderName()),
                            Utils.line("Completed"),
                            Utils.line("Click to start tutorial again"));
            } else {
                if (bStaffView)
                    recommendationIcon = teachingtutorials.utils.Utils.createItem(Material.BOOK, 1,
                            Utils.title(tutorialRecommendation.getTutorialName(Network.getInstance().getTutorialsDBConnection(), LOGGER)),
                            Utils.line("Recommended by " + tutorialRecommendation.getRecommenderName()));
                else
                    recommendationIcon = teachingtutorials.utils.Utils.createItem(Material.BOOK, 1,
                            Utils.title(tutorialRecommendation.getTutorialName(Network.getInstance().getTutorialsDBConnection(), LOGGER)),
                            Utils.line("Recommended by " + tutorialRecommendation.getRecommenderName()),
                            Utils.line("Click to start tutorial"));
            }

            super.setItem(i - iStart, recommendationIcon, new guiAction() {
                @Override
                public void click(NetworkUser u) {
                    if (!bStaffView && ownerUUID != null) {
                        DBConnection dbConnection = Network.getInstance().getTutorialsDBConnection();
                        startTutorial(Network.getInstance(), LessonObject.getUnfinishedLessonsOfPlayer(ownerUUID, dbConnection, LOGGER),
                                user, RecommendedTutorialsGui.this, Tutorial.fetchByTutorialID(tutorialRecommendation.getTutorialID(), dbConnection, LOGGER),
                                null);
                    }
                }
            });
        }

        int iOrder = i;

        // Add the + button - will open the add menu
        if (iPage == iPages && bStaffView & ownerUUID != null) { // If on last page and staff view

            int iLocationOfAdd = iOrder - iStart;

            super.setItem(iLocationOfAdd,
                    Utils.createItem(Material.WRITABLE_BOOK, 1,
                            Utils.title("Add Recommendation"),
                            Utils.line("Click to recommend a tutorial")),
                    new guiAction() {
                        @Override
                        public void click(NetworkUser u) {
                            // Opens the new recommendation menu
                            RecommendationAddGui add = new RecommendationAddGui(RecommendedTutorialsGui.this, user, iPlotID, ownerUUID, tutorialRecommendations);
                            add.open(user);
                        }
                    });
        }
    }

    public static void startTutorial(Network plugin, LessonObject[] lessons, NetworkUser user, Gui parentGui, Tutorial tutorialToStart, Location locationToStart) {
        boolean bLessonFound = false;

        for (LessonObject lesson : lessons) {
            if (tutorialToStart.getTutorialID() == lesson.getTutorialID()) {
                if (locationToStart == null) {
                    bLessonFound = true;
                    LessonContinueConfirmer confirmer = new LessonContinueConfirmer(plugin, user, parentGui, lesson, "You have a lesson for this tutorial already");
                    confirmer.open(user);
                    break;
                }

                if (locationToStart.getLocationID() == lesson.getLocation().getLocationID()) {
                    bLessonFound = true;
                    LessonContinueConfirmer confirmer = new LessonContinueConfirmer(plugin, user, parentGui, lesson, "You have a lesson at this location already");
                    confirmer.open(user);
                    break;
                }
            }
        }

        if (!bLessonFound) {
            if (locationToStart == null) {
                if (Event.addEvent(
                        EventType.START_TUTORIAL, user.player.getUniqueId(), tutorialToStart.getTutorialID(), Network.getInstance().getTutorialsDBConnection(), Constants.LOGGER)) {
                    SwitchServer.switchServer(user.player, Network.getInstance().getGlobalSQL().getString("SELECT name FROM server_data WHERE type='TUTORIAL';"));
                    user.player.closeInventory();
                } else {
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
                }
            } else if (Event.addEvent(EventType.START_LOCATION, user.player.getUniqueId(), locationToStart.getLocationID(), Network.getInstance().getTutorialsDBConnection(),
                    Constants.LOGGER)) {
                SwitchServer.switchServer(user.player, Network.getInstance().getGlobalSQL().getString("SELECT name FROM server_data WHERE type='TUTORIAL';"));
                user.player.closeInventory();
            } else {
                user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            }
        }

    }

    @Override
    public void refresh() {
        clearGui();

        // Fetches the tutorial recommendations for this plot
        tutorialRecommendations = Network.getInstance().getPlotSQL().fetchTutorialRecommendationsForPlot(LOGGER, iPlotID);

        this.iPages = ((tutorialRecommendations.length - 1) / 45) + 1;
        this.iPage = 1;

        addItems();
    }
}