package net.bteuk.network.gui.tutorials;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkGui;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.tutorialobjects.TutorialRecommendation;

@Log
public class RecommendedTutorialsMenu extends AbstractTutorialsGui {
    /**
     * A reference to the instance of the Network plugin
     */
    private final Network plugin;

    /**
     * The user whom this menu is for
     */
    private final NetworkUser user;

    /**
     * A reference to the parent Gui
     */
    private final NetworkGui parentGui;

    /**
     * The list of tutorial recommendations which this menu is displaying
     */
    private final TutorialRecommendation[] tutorialRecommendations;

    /**
     * The number of pages in this menu
     */
    private final int iPages;

    /**
     * The current page that the player is on
     */
    private int iPage;

    public RecommendedTutorialsMenu(GuiProvider provider, TutorialsGui mainMenu, NetworkUser user, TutorialRecommendation[] tutorialRecommendations) {
        super(provider, 54, Utils.title("Recommended Tutorials"));
        this.plugin = provider.instance();
        this.parentGui = mainMenu;
        this.user = user;
        this.tutorialRecommendations = tutorialRecommendations;

        this.iPages = ((tutorialRecommendations.length - 1) / 36) + 1;
    }

    protected void createGui() {
        // Reset page
        this.iPage = 1;

        // Back button
        setItem(53, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Back to Main Menu")), (NetworkUser u) -> {
            user.mainGui = parentGui;
            user.mainGui.open(user.player);
            delete();
        });

        // Indicates that there are no tutorial recommendations
        if (tutorialRecommendations.length == 0) {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1, Utils.title("You have no tutorial recommendations!"));
            setItem(5 - 1, noTutorials);
        }

        // Adds the tutorials
        int iStart = (iPage - 1) * 9;
        int iMax = Math.min((iPage + 3) * 9, tutorialRecommendations.length);
        for (int i = iStart; i < iMax; i++) {
            // Fetches the tutorial
            Tutorial tutorial = Tutorial.fetchByTutorialID(tutorialRecommendations[i].getTutorialID(), provider.tutorialsDBConnection(), plugin.getLogger());
            if (tutorial == null) continue;
            if (!tutorial.isInUse()) continue;

            final Location location;
            if (tutorialRecommendations[i].getLocationID() > 0)
                location = Location.getLocationByLocationID(provider.tutorialsDBConnection(), plugin.getLogger(), tutorialRecommendations[i].getLocationID());
            else location = null;

            ItemStack lesson = Utils.createItem(Material.KNOWLEDGE_BOOK, 1, Utils.title(tutorial.getTutorialName()),
                    Utils.line("Recommended by " + tutorialRecommendations[i].getRecommenderName()));

            super.setItem(i - iStart, lesson,
                    (NetworkUser u) -> startTutorial(LessonObject.getUnfinishedLessonsOfPlayer(user.player.getUniqueId(), provider.tutorialsDBConnection(), plugin.getLogger()),
                            user, this, tutorial, location, log));
        }

        // Page back
        if (iPage > 1) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2", Material.ACACIA_BOAT, 1,
                    Utils.title("Page back"));
            super.setItem(45, pageBack, (NetworkUser u) -> {
                iPage--;
                refresh();
                updatePlayerInventory(u.player);
            });
        }

        // Page forwards
        if (iPage < iPages) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44", Material.ACACIA_BOAT, 1,
                    Utils.title("Page forwards"));
            super.setItem(53, pageBack, (NetworkUser u) -> {
                iPage++;
                refresh();
                updatePlayerInventory(u.player);
            });
        }
    }
}
