package net.bteuk.network.gui.tutorials;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.entity.Role;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.EventType;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.tutorialobjects.TutorialRecommendation;
import teachingtutorials.tutorialplaythrough.Lesson;
import teachingtutorials.utils.User;

@Log
public class TutorialsGui extends AbstractTutorialsGui {

    private final Network plugin;

    private final NetworkUser user;

    /**
     * A reference to the user for which this menu is for
     */
    private final User tutorialsUser;

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

    private final Role applicant;

    public TutorialsGui(GuiProvider provider, NetworkUser user) {

        super(provider, 27, Component.text("Tutorials Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.plugin = provider.instance();
        this.user = user;
        this.applicant = provider.roles().getRoleById("applicant");

        // Gets the information about the Tutorials User
        tutorialsUser = new User(this.user.player);
        tutorialsUser.fetchDetailsByUUID(provider.tutorialsDBConnection(), log);
        tutorialsUser.calculateRatings(provider.tutorialsDBConnection());
    }

    private void fetchInformation() {
        // Get compulsory tutorial
        Tutorial[] compulsoryTutorials = Tutorial.fetchAll(true, true, null, provider.tutorialsDBConnection(), plugin.getLogger());
        if (compulsoryTutorials.length == 0) compulsoryTutorial = null;
        else compulsoryTutorial = compulsoryTutorials[0];

        // Get the current unfinished lessons of the player
        currentLessons = LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), provider.tutorialsDBConnection(), plugin.getLogger());

        // Get the next tutorial for this player
        nextTutorial = Lesson.decideTutorial(tutorialsUser, provider.tutorialsDBConnection(), plugin.getLogger());
    }

    /**
     * Creates the icons and actions for this menu
     */
    protected void createGui() {

        // Checks the system has the compulsory tutorial feature enabled and the user hasn't completed the compulsory tutorial
        if (provider.constants().compulsoryTutorial() && compulsoryTutorial != null && !tutorialsUser.bHasCompletedCompulsory) {
            // Check if they have started the compulsory
            LessonObject compulsoryLesson = null;
            for (LessonObject lesson : currentLessons) {
                if (lesson.getTutorialID() == compulsoryTutorial.getTutorialID()) {
                    compulsoryLesson = lesson;
                    break;
                }
            }

            if (compulsoryLesson == null) compulsoryNeverStarted();
            else compulsoryNotFinished(compulsoryLesson);
        } else
            // User has not completed the compulsory tutorial or doesn't need to
            compulsoryFinished();

        // Admin and creator menu
        if (user.player.hasPermission("TeachingTutorials.Admin") || user.player.hasPermission("TeachingTutorials.Creator")) {
            // Admin and creator menu
            super.setItem(19 - 1, teachingtutorials.utils.Utils.createItem(Material.LECTERN, 1, Utils.title("Admin Menu"), Utils.line("Teleport to the tutorials server")),
                    this::switchServer);
        }

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the navigator main menu.")), (NetworkUser user) -> {

            // Delete this gui.
            this.delete();
            user.mainGui = null;

            // Switch to the navigation menu.
            provider.navigator().openMainMenu(user);
        });
    }

    /**
     * Adds the menu items for if the compulsory tutorial has never been started
     */
    private void compulsoryNeverStarted() {
        ItemStack beginCompulsory = teachingtutorials.utils.Utils.createItem(Material.BOOK, 1, Utils.title("Begin the Starter Tutorial"),
                Utils.line("Gain the ").append(applicant == null ? Utils.line("Applicant") : applicant.getColouredRoleName()).append(Utils.line("rank!")));

        super.setItem(14 - 1, beginCompulsory, (NetworkUser u) -> startTutorial(currentLessons, u, this, compulsoryTutorial, null, log));

    }

    /**
     * Adds the menu items for if the compulsory tutorial has been started but never finished
     *
     * @param compulsoryLesson The lesson object for the compulsory tutorial lesson they currently have ongoing
     */
    private void compulsoryNotFinished(LessonObject compulsoryLesson) {
        // Restart compulsory
        ItemStack restartCompulsory = teachingtutorials.utils.Utils.createItem(Material.BOOK, 1, Utils.title("Restart the Starter Tutorial"),
                Utils.line("Gain the ").append(applicant == null ? Utils.line("Applicant") : applicant.getColouredRoleName()).append(Utils.line("rank!")));

        super.setItem(12 - 1, restartCompulsory, (NetworkUser u) -> {
            // Switch to the tutorial.
            if (Event.addEvent(EventType.RESTART_LESSON, user.player.getUniqueId(), compulsoryLesson.getLessonID(), provider.tutorialsDBConnection(), log)) switchServer(user);
            else user.sendMessage(Utils.error("A problem occurred, please let staff know"));
        });

        // Resume compulsory
        ItemStack resumeCompulsory = teachingtutorials.utils.Utils.createItem(Material.WRITABLE_BOOK, 1, Utils.title("Resume the Starter Tutorial"),
                Utils.line("Gain the ").append(applicant == null ? Utils.line("Applicant") : applicant.getColouredRoleName()).append(Utils.line("rank!")));

        super.setItem(16 - 1, resumeCompulsory, (NetworkUser u) -> {
            // Switch to the tutorial.
            if (Event.addEvent(EventType.CONTINUE_LESSON, user.player.getUniqueId(), compulsoryLesson.getLessonID(), provider.tutorialsDBConnection(), log)) switchServer(user);
            else user.sendMessage(Utils.error("A problem occurred, please let staff know"));
        });
    }

    /**
     * Adds the menu items for if the compulsory tutorial has been completed, and the main tutorials system is unlocked
     */
    private void compulsoryFinished() {
        // Compulsory tutorial
        ItemStack compulsory = teachingtutorials.utils.Utils.createItem(Material.JUNGLE_DOOR, 1, Utils.title("Redo the Starter Tutorial"),
                Utils.line("Refresh your essential knowledge"));

        super.setItem(9, compulsory, (NetworkUser u) -> startTutorial(currentLessons, u, this, compulsoryTutorial, null, log));

        //---------- Library Option ----------
        ItemStack tutorialLibrary = teachingtutorials.utils.Utils.createItem(Material.BOOKSHELF, 1, Utils.title("Tutorial Library"),
                Utils.line("Browse all of our available tutorials"));

        super.setItem(11, tutorialLibrary, (NetworkUser u) -> {
            user.mainGui = new TutorialLibraryGui(provider, user,
                    LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), provider.tutorialsDBConnection(), plugin.getLogger()));
            user.mainGui.open(user.player);
            delete();
        });

        // Current lessons
        ItemStack currentLessons = teachingtutorials.utils.Utils.createItem(Material.WRITABLE_BOOK, 1, Utils.title("Current Lessons"), Utils.line("View your unfinished lessons"));
        super.setItem(13, currentLessons, (NetworkUser u) -> {
            user.mainGui = new LessonsMenu(provider, user, this, TutorialsGui.this.currentLessons);
            user.mainGui.open(user.player);
        });

        // Tutorial recommendations
        super.setItem(15, Utils.createItem(Material.CHEST, 1, Utils.title("Recommended Tutorials")), (NetworkUser u) -> {
            user.mainGui = new RecommendedTutorialsMenu(provider, this, user,
                    TutorialRecommendation.fetchTutorialRecommendationsForPlayer(provider.tutorialsDBConnection(), plugin.getLogger(), user.player.getUniqueId()));
            user.mainGui.open(user.player);
        });

        // Continue learning/next tutorial
        ItemStack continueLearning = teachingtutorials.utils.Utils.createItem(Material.END_CRYSTAL, 1, Utils.title("Start a new Tutorial:"),
                Utils.line(nextTutorial.getTutorialName()));

        if (nextTutorial != null) {
            super.setItem(17, continueLearning, (NetworkUser u) -> startTutorial(this.currentLessons, u, this, nextTutorial, null, log));
        }
    }

    @Override
    public void refresh() {
        this.clear();
        fetchInformation();
        createGui();
    }
}
