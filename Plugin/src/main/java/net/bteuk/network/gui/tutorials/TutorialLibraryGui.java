package net.bteuk.network.gui.tutorials;

import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.EventType;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;

import static net.bteuk.network.utils.Constants.LOGGER;

public class TutorialLibraryGui extends Gui {

    private final Network plugin;

    private final NetworkUser user;

    private Tutorial[] inUseTutorials;

    /**
     * The list of lessons that this player has ongoing
     */
    private final LessonObject[] lessons;

    private static final Component inventoryName = Utils.title("Library");

    public TutorialLibraryGui(Network plugin, NetworkUser user, Tutorial[] inUseTutorials, LessonObject[] userCurrentLessons) {

        // Initialises the Gui with the menu icons already set
        super(getGUI(inUseTutorials));

        this.plugin = plugin;
        this.user = user;
        this.inUseTutorials = inUseTutorials;
        this.lessons = userCurrentLessons;

        // Sets the actions of the menu
        setActions();
    }

    /**
     * Creates an inventory with icons representing a library of available tutorials
     *
     * @param tutorials A list of all in-use tutorials
     * @return An inventory of icons
     */
    public static Inventory getGUI(Tutorial[] tutorials) {
        // Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        Inventory inventory;

        // Works out how many rows in the inventory are needed
        iTutorials = tutorials.length;
        iDiv = iTutorials / 9;
        iMod = iTutorials % 9;

        if (iMod != 0 || iDiv == 0) {
            iDiv = iDiv + 1;
        }

        // Enables an empty row and then a row for the back button
        iRows = iDiv + 2;

        // Create inventory
        inventory = Bukkit.createInventory(null, iRows * 9);
        inventory.clear();

        Inventory toReturn = Bukkit.createInventory(null, iRows * 9, inventoryName);

        // Indicates that there are no tutorials in the system
        if (iTutorials == 0) {
            ItemStack noTutorials = teachingtutorials.utils.Utils.createItem(Material.BARRIER, 1,
                    Utils.title("There are no tutorials available to play currently"),
                    Utils.line("Ask a server admin to get some created"));
            inventory.setItem(5 - 1, noTutorials);
        }

        // Adds the tutorials to the menu options
        // Inv slot 0 = the first one
        ItemStack tutorial;
        for (i = 0; i < iTutorials; i++) {
            tutorial = teachingtutorials.utils.Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                    Utils.title(tutorials[i].getTutorialName()).decoration(TextDecoration.BOLD, true),
                    Utils.line("Tutor - " + Bukkit.getOfflinePlayer(tutorials[i].getUUIDOfAuthor()).getName()));
            inventory.setItem(i, tutorial);
        }

        // Adds a back button
        ItemStack back = teachingtutorials.utils.Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Back to main menu"));
        inventory.setItem((iRows * 9) - 1, back);

        toReturn.setContents(inventory.getContents());
        return toReturn;
    }

    /**
     * Adds the click-actions to the menu slots of this library menu
     */
    private void setActions() {
        // Declare variables
        int i;
        int iTutorials;
        int iDiv;
        int iMod;
        int iRows;

        // Works out how many rows in the inventory are needed
        iTutorials = inUseTutorials.length;
        iDiv = iTutorials / 9;
        iMod = iTutorials % 9;

        if (iMod != 0 || iDiv == 0) {
            iDiv = iDiv + 1;
        }

        // Enables an empty row and then a row for the back button
        iRows = iDiv + 2;

        // Adds back button
        setAction((iRows * 9) - 1, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                delete();
                u.mainGui = new TutorialsGui(user);
                u.mainGui.open(u.player);
            }
        });

        // Inv slot 0 = the first one
        // Adds the actions of each slot
        for (i = 0; i < inUseTutorials.length; i++) {
            int iSlot = i;
            setAction(iSlot, new Gui.guiAction() {
                @Override
                public void click(NetworkUser u) {
                    startTutorial(plugin, lessons, user, TutorialLibraryGui.this, inUseTutorials[iSlot], null);
                }
            });
        }
    }

    /**
     * Handles the logic when a player wishes to start a specific tutorial
     *
     * @param plugin          A reference to the instance of the TeachingTutorials plugin
     * @param lessons         A list of unfinished lessons for the given player
     * @param user            A reference to the user who wishes to start a specific tutorial
     * @param parentGui       A reference to the parent gui which to return back
     * @param tutorialToStart A reference to the Tutorial that the player wishes to start
     * @param locationToStart A reference to the Location that a player wishes to start, if specified
     * @return
     */
    public static void startTutorial(Network plugin, LessonObject[] lessons, NetworkUser user, Gui parentGui, Tutorial tutorialToStart, Location locationToStart) {
        // Check whether the player already has a current lesson for this tutorial
        boolean bLessonFound = false;
        for (LessonObject lesson : lessons) {
            if (tutorialToStart.getTutorialID() == lesson.getTutorialID()) {
                // Open confirmation menu
                // If location matters then check that
                if (locationToStart != null) {
                    if (locationToStart.getLocationID() == lesson.getLocation().getLocationID()) {
                        bLessonFound = true;

                        user.mainGui = new LessonContinueConfirmer(plugin, user, parentGui, lesson, "You have a lesson at this location already");
                        user.mainGui.open(user);

                        // Break, let the other menu take over
                        break;
                    }
                } else {
                    bLessonFound = true;
                    // If not then open confirmation menu
                    user.mainGui = new LessonContinueConfirmer(plugin, user, parentGui, lesson, "You have a lesson for this tutorial already");
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
                    TutorialsGui.switchServer(user);
                else
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            } else {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.START_LOCATION, user.player.getUniqueId(), locationToStart.getLocationID(),
                        Network.getInstance().getTutorialsDBConnection(), LOGGER))
                    TutorialsGui.switchServer(user);
                else
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
            }
        }
    }

    @Override
    public void refresh() {
        // Refresh List of available tutorials
        this.inUseTutorials =
                Tutorial.getInUseTutorialsWithLocations(Network.getInstance().getTutorialsDBConnection(), LOGGER);

        // Refresh icons
        this.clearGui();
        Inventory inventory = TutorialLibraryGui.getGUI(this.inUseTutorials);
        this.getInventory().setContents(inventory.getContents());

        // Refresh actions
        setActions();
    }
}
