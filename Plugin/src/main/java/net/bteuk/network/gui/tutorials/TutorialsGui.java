package net.bteuk.network.gui.tutorials;

import net.bteuk.network.Network;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Role;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.EventType;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.tutorialobjects.TutorialRecommendation;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.utils.User;

import static net.bteuk.network.utils.Constants.LOGGER;
import static net.bteuk.network.utils.NetworkConfig.CONFIG;

public class TutorialsGui extends NetworkRefreshableGui {

    private final Network plugin = Network.getInstance();

    private final NetworkUser user;

    /**
     * A reference to the user for which this menu is for
     */
    private final User tutorialsUser;

    /**
     * Gets the compulsory tutorial setting
     */
    private final boolean bCompulsoryTutorialEnabled = CONFIG.getBoolean("tutorials.compulsory_tutorial");

    /**
     * The Tutorial of the compulsory tutorial. Null if no compulsory tutorial is set
     */
    private Tutorial compulsoryTutorial;

    /**
     * A list of the current lessons that a player has ongoing
     **/
    private LessonObject[] currentLessons;

    /**
     * The next tutorial which a player would play if clicking continue learning
     */
    private Tutorial nextTutorial;

    Role applicant = Roles.getRoleById("applicant");

    public TutorialsGui(NetworkUser user) {

        super(27, Component.text("Tutorials Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;

        // Gets the information about the Tutorials User
        tutorialsUser = new User(this.user.player);
        tutorialsUser.fetchDetailsByUUID(Network.getInstance().getTutorialsDBConnection(), LOGGER);
        tutorialsUser.calculateRatings(Network.getInstance().getTutorialsDBConnection());

        fetchInformation();

        createGui();
    }

    private void fetchInformation() {
        // Get compulsory tutorial
        Tutorial[] compulsoryTutorials = Tutorial.fetchAll(true, true, null, plugin.getTutorialsDBConnection(), plugin.getLogger());
        if (compulsoryTutorials.length == 0)
            compulsoryTutorial = null;
        else
            compulsoryTutorial = compulsoryTutorials[0];

        // Get the current unfinished lessons of the player
        currentLessons = LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), plugin.getTutorialsDBConnection(), plugin.getLogger());

        // Get the next tutorial for this player
        nextTutorial = Lesson.decideTutorial(tutorialsUser, plugin.getTutorialsDBConnection(), plugin.getLogger());
    }

    protected static void switchServer(NetworkUser user) {
        SwitchServer.switchServer(user.player, Network.getInstance().getGlobalSQL().getString("SELECT name FROM " +
                "server_data WHERE type='TUTORIAL';"));
        user.player.closeInventory();
    }

    /**
     * Creates the icons and actions for this menu
     */
    private void createGui() {

        // Checks the system has the compulsory tutorial feature enabled and the user hasn't completed the compulsory tutorial
        if (bCompulsoryTutorialEnabled && compulsoryTutorial != null && !tutorialsUser.bHasCompletedCompulsory) {
            // Check if they have started the compulsory
            LessonObject compulsoryLesson = null;
            for (LessonObject lesson : currentLessons) {
                if (lesson.getTutorialID() == compulsoryTutorial.getTutorialID()) {
                    compulsoryLesson = lesson;
                    break;
                }
            }

            if (compulsoryLesson == null)
                compulsoryNeverStarted();
            else
                compulsoryNotFinished(compulsoryLesson);
        } else
            // User has not completed compulsory tutorial or doesn't need to
            compulsoryFinished();

        // Admin and creator menu
        if (user.player.hasPermission("TeachingTutorials.Admin") || user.player.hasPermission("TeachingTutorials.Creator")) {
            // Admin and creator menu
            super.setItem(19 - 1, teachingtutorials.utils.Utils.createItem(Material.LECTERN, 1,
                    Utils.title("Admin Menu"), Utils.line("Teleport to the tutorials server")), user ->
            {
                switchServer(user);
            });
        }

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the navigator main menu.")),
                user ->

                {

                    // Delete this gui.
                    this.delete();
                    user.mainGui = null;

                    // Switch to navigation menu.
                    Network.getInstance().navigatorGui.open(user);
                });
    }

    /**
     * Adds the menu items for if the compulsory tutorial has never been started
     */
    private void compulsoryNeverStarted() {
        ItemStack beginCompulsory = teachingtutorials.utils.Utils.createItem(Material.BOOK, 1,
                Utils.title("Begin the Starter Tutorial"),
                Utils.line("Gain the ").append(applicant == null ? Utils.line("Applicant") :
                                applicant.getColouredRoleName())
                        .append(Utils.line("rank!")));

        super.setItem(14 - 1, beginCompulsory, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                startTutorial(compulsoryTutorial, null);
            }
        });

    }

    /**
     * Adds the menu items for if the compulsory tutorial has been started but never finished
     *
     * @param compulsoryLesson The lesson object for the compulsory tutorial lesson they currently have ongoing
     */
    private void compulsoryNotFinished(LessonObject compulsoryLesson) {
        // Restart compulsory
        ItemStack restartCompulsory = teachingtutorials.utils.Utils.createItem(Material.BOOK, 1,
                Utils.title("Restart the Starter Tutorial"),
                Utils.line("Gain the ").append(applicant == null ? Utils.line("Applicant") :
                                applicant.getColouredRoleName())
                        .append(Utils.line("rank!")));

        super.setItem(12 - 1, restartCompulsory, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.RESTART_LESSON, user.player.getUniqueId(), compulsoryLesson.getLessonID(),
                        Network.getInstance().getTutorialsDBConnection(), LOGGER))
                    switchServer(user);
                else
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            }
        });

        // Resume compulsory
        ItemStack resumeCompulsory = teachingtutorials.utils.Utils.createItem(Material.WRITABLE_BOOK, 1,
                Utils.title("Resume the Starter Tutorial"),
                Utils.line("Gain the ").append(applicant == null ? Utils.line("Applicant") :
                                applicant.getColouredRoleName())
                        .append(Utils.line("rank!")));

        super.setItem(16 - 1, resumeCompulsory, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.CONTINUE_LESSON, user.player.getUniqueId(), compulsoryLesson.getLessonID(),
                        Network.getInstance().getTutorialsDBConnection(), LOGGER))
                    switchServer(user);
                else
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            }
        });
    }

    /**
     * Adds the menu items for if the compulsory tutorial has been completed, and the main tutorials system is unlocked
     */
    private void compulsoryFinished() {
        // Compulsory tutorial
        ItemStack compulsory = teachingtutorials.utils.Utils.createItem(Material.JUNGLE_DOOR, 1,
                Utils.title("Redo the Starter Tutorial"),
                Utils.line("Refresh your essential knowledge"));

        super.setItem(9, compulsory, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                startTutorial(compulsoryTutorial, null);
            }
        });

        //---------- Library Option ----------
        ItemStack tutorialLibrary = teachingtutorials.utils.Utils.createItem(Material.BOOKSHELF, 1,
                Utils.title("Tutorial Library"),
                Utils.line("Browse all of our available tutorials"));

        super.setItem(11, tutorialLibrary, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                user.mainGui = new TutorialLibraryGui(plugin, user, Tutorial.getInUseTutorialsWithLocations(plugin.getTutorialsDBConnection(), plugin.getLogger()),
                        LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), plugin.getTutorialsDBConnection(), plugin.getLogger()));
                user.mainGui.open(user);
                delete();
            }
        });

        // Current lessons
        ItemStack currentLessons = teachingtutorials.utils.Utils.createItem(Material.WRITABLE_BOOK, 1,
                Utils.title("Current Lessons"),
                Utils.line("View your unfinished lessons"));
        super.setItem(13, currentLessons, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                user.mainGui = new LessonsMenu(plugin, user, TutorialsGui.this, TutorialsGui.this.currentLessons);
                user.mainGui.open(user);
            }
        });

        // Tutorial recommendations
        super.setItem(15, Utils.createItem(Material.CHEST, 1, Utils.title("Recommended Tutorials")),
                new guiAction() {
                    @Override
                    public void click(NetworkUser u) {
                        user.mainGui = new RecommendedTutorialsMenu(plugin, TutorialsGui.this, user,
                                TutorialRecommendation.fetchTutorialRecommendationsForPlayer(plugin.getTutorialsDBConnection(), plugin.getLogger(), user.player.getUniqueId()));
                        user.mainGui.open(user);
                    }
                });

        // Continue learning/next tutorial
        ItemStack continueLearning = teachingtutorials.utils.Utils.createItem(Material.END_CRYSTAL, 1,
                Utils.title("Start a new Tutorial:"),
                Utils.line(nextTutorial.getTutorialName()));

        if (nextTutorial != null)
            super.setItem(17, continueLearning, new Gui.guiAction() {
                @Override
                public void click(NetworkUser u) {
                    startTutorial(nextTutorial, null);
                }
            });
    }

    /**
     * Handles the logic when a player wishes to start a tutorial
     *
     * @param tutorialToStart A reference to the Tutorial that the player wishes to start
     * @return
     */
    public void startTutorial(Tutorial tutorialToStart, Location locationToStart) {
        // Check whether the player already has a current lesson for this tutorial
        boolean bLessonFound = false;
        for (LessonObject lesson : currentLessons) {
            if (tutorialToStart.getTutorialID() == lesson.getTutorialID()) {
                // Open confirmation menu
                // If location matters then check that
                if (locationToStart != null) {
                    if (locationToStart.getLocationID() == lesson.getLocation().getLocationID()) {
                        bLessonFound = true;

                        user.mainGui = new LessonContinueConfirmer(plugin, user, this, lesson, "You have a lesson at this location already");
                        user.mainGui.open(user);

                        // Break, let the other menu take over
                        break;
                    }
                } else {
                    bLessonFound = true;
                    // If not then open confirmation menu
                    user.mainGui = new LessonContinueConfirmer(plugin, user, this, lesson, "You have a lesson for this tutorial already");
                    user.mainGui.open(user);

                    // Break, let the other menu take over
                    break;
                }
            }
        }

        // If player doesn't have current lesson for this tutorial then create a new one
        if (!bLessonFound) {
            if (locationToStart == null) {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.START_TUTORIAL, user.player.getUniqueId(), tutorialToStart.getTutorialID(),
                        Network.getInstance().getTutorialsDBConnection(), LOGGER))
                    switchServer(user);
                else
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            } else {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.START_LOCATION, user.player.getUniqueId(), locationToStart.getLocationID(),
                        Network.getInstance().getTutorialsDBConnection(), LOGGER))
                    switchServer(user);
                else
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            }
        }
    }

    public void refresh() {

        this.clearGui();
        fetchInformation();
        createGui();
    }
}
