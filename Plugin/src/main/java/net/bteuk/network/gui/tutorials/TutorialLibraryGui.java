package net.bteuk.network.gui.tutorials;

import lombok.extern.java.Log;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Tutorial;

@Log
public class TutorialLibraryGui extends AbstractTutorialsGui {

    private final NetworkUser user;

    private Tutorial[] inUseTutorials;

    /**
     * The list of lessons that this player has ongoing
     */
    private final LessonObject[] lessons;

    private static final Component inventoryName = Utils.title("Library");

    public TutorialLibraryGui(GuiProvider provider, NetworkUser user, LessonObject[] userCurrentLessons) {

        // Initialises the Gui with the menu icons already set
        super(provider, getGUI(Tutorial.getInUseTutorialsWithLocations(provider.tutorialsDBConnection(), log)));

        this.user = user;
        this.lessons = userCurrentLessons;
    }

    /**
     * Creates an inventory with icons representing a library of available tutorials
     *
     * @param tutorials A list of all in-use tutorials
     * @return An inventory of icons
     */
    private static Inventory getGUI(Tutorial[] tutorials) {
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
            ItemStack noTutorials = teachingtutorials.utils.Utils.createItem(Material.BARRIER, 1, Utils.title("There are no tutorials available to play currently"),
                    Utils.line("Ask a server admin to get some created"));
            inventory.setItem(5 - 1, noTutorials);
        }

        // Adds the tutorials to the menu options
        // Inv slot 0 = the first one
        ItemStack tutorial;
        for (i = 0; i < iTutorials; i++) {
            tutorial = teachingtutorials.utils.Utils.createItem(Material.KNOWLEDGE_BOOK, 1, Utils.title(tutorials[i].getTutorialName()).decoration(TextDecoration.BOLD, true),
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
        setAction((iRows * 9) - 1, (NetworkUser u) -> {
            delete();
            u.mainGui = new TutorialsGui(provider, user);
            u.mainGui.open(u.player);
        });

        // Inv slot 0 = the first one
        // Adds the actions of each slot
        for (i = 0; i < inUseTutorials.length; i++) {
            int iSlot = i;
            setAction(iSlot, (NetworkUser u) -> startTutorial(lessons, user, TutorialLibraryGui.this, inUseTutorials[iSlot], null, log));
        }
    }

    @Override
    protected void createGui() {
        // Refresh the list of available tutorials
        this.inUseTutorials = Tutorial.getInUseTutorialsWithLocations(provider.tutorialsDBConnection(), log);

        Inventory inventory = getGUI(this.inUseTutorials);
        setItemsFromInventory(inventory);

        // Refresh actions
        setActions();
    }
}
