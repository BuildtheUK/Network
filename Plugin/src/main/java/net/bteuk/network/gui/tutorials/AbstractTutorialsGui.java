package net.bteuk.network.gui.tutorials;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkGui;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.EventType;
import teachingtutorials.tutorialobjects.LessonObject;
import teachingtutorials.tutorialobjects.Location;
import teachingtutorials.tutorialobjects.Tutorial;

import java.util.logging.Logger;

public abstract class AbstractTutorialsGui extends NetworkRefreshableGui {

    public AbstractTutorialsGui(GuiProvider provider, int inventorySize, Component inventoryName) {
        super(provider, inventorySize, inventoryName);
    }

    public AbstractTutorialsGui(GuiProvider provider, Inventory inventory) {
        super(provider, inventory);
    }

    void switchServer(NetworkUser user) {
        provider.serverAPI().switchServer(PlayerAdapter.adapt(user.player), provider.globalSQL().getString("SELECT name FROM server_data WHERE type='TUTORIAL';"));
        user.player.closeInventory();
    }

    /**
     * Handles the logic when a player wishes to start a specific tutorial
     *
     * @param lessons         A list of unfinished lessons for the given player
     * @param user            A reference to the user who wishes to start a specific tutorial
     * @param parentGui       A reference to the parent gui which to return back
     * @param tutorialToStart A reference to the Tutorial that the player wishes to start
     * @param locationToStart A reference to the Location that a player wishes to start, if specified
     */
    void startTutorial(LessonObject[] lessons, NetworkUser user, NetworkGui parentGui, Tutorial tutorialToStart, Location locationToStart, Logger log) {
        // Check whether the player already has a current lesson for this tutorial
        boolean bLessonFound = false;
        for (LessonObject lesson : lessons) {
            if (tutorialToStart.getTutorialID() == lesson.getTutorialID()) {
                // Open confirmation menu
                // If location matters then check that
                if (locationToStart != null) {
                    if (locationToStart.getLocationID() == lesson.getLocation().getLocationID()) {
                        bLessonFound = true;

                        user.mainGui = new LessonContinueConfirmer(provider, user, parentGui, lesson, "You have a lesson at this location already");
                        user.mainGui.open(user.player);

                        // Break, let the other menu take over
                        break;
                    }
                } else {
                    bLessonFound = true;
                    // If not then open confirmation menu
                    user.mainGui = new LessonContinueConfirmer(provider, user, parentGui, lesson, "You have a lesson for this tutorial already");
                    user.mainGui.open(user.player);

                    // Break, let the other menu take over
                    break;
                }
            }
        }

        // If the player doesn't have a current lesson for this tutorial, then create a new one
        if (!bLessonFound) {
            if (locationToStart == null) {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.START_TUTORIAL, user.player.getUniqueId(), tutorialToStart.getTutorialID(), provider.tutorialsDBConnection(), log)) {
                    switchServer(user);
                } else {
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
                }
            } else {
                // Switch to the tutorial.
                if (Event.addEvent(EventType.START_LOCATION, user.player.getUniqueId(), locationToStart.getLocationID(), provider.tutorialsDBConnection(), log)) {
                    switchServer(user);
                } else {
                    user.sendMessage(Utils.error("A problem occurred, please let staff know"));
                }
            }
        }
    }
}
