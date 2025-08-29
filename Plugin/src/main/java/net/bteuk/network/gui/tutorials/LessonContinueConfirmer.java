package net.bteuk.network.gui.tutorials;

import lombok.extern.java.Log;
import net.bteuk.minecraft.gui.Gui;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.guis.Event;
import teachingtutorials.guis.EventType;
import teachingtutorials.tutorialobjects.LessonObject;

@Log
public class LessonContinueConfirmer extends AbstractTutorialsGui {

    /**
     * The user whom this menu is for
     */
    private final NetworkUser user;

    /**
     * The Lesson which is to be restarted or resumed
     */
    private final LessonObject lessonToContinue;

    /**
     * The message to display to the user
     */
    private final String szMessage;

    /**
     * A reference to the parent Gui
     */
    private final Gui parentGui;

    /**
     * @param provider         Provider of gui dependencies
     * @param user             The user whom this menu is for
     * @param lessonToContinue The Lesson which is to be restarted or resumed
     * @param szMessage        The message to display to the user
     */
    public LessonContinueConfirmer(GuiProvider provider, NetworkUser user, Gui parentGui, LessonObject lessonToContinue, String szMessage) {
        super(provider, 27, Utils.title("Resume or continue lesson?"));
        this.parentGui = parentGui;
        this.user = user;
        this.lessonToContinue = lessonToContinue;
        this.szMessage = szMessage;
    }

    /**
     * Adds the icons and actions to the menu
     */
    protected void createGui() {
        // Info
        super.setItem(4, Utils.createItem(Material.KNOWLEDGE_BOOK, 1,
                Utils.line(szMessage)));

        // Restart lesson
        super.setItem(12 - 1, Utils.createItem(Material.BOOK, 1,
                Utils.title("Restart the lesson")), (NetworkUser u) -> {
            // Switch to the tutorial.
            if (Event.addEvent(EventType.RESTART_LESSON, user.player.getUniqueId(), lessonToContinue.getLessonID(),
                    provider.tutorialsDBConnection(), log)) {
                switchServer(user);
            } else {
                user.sendMessage(net.bteuk.network.utils.Utils.error("A problem occurred, please let staff know"));
            }
        });

        // Resume compulsory
        ItemStack resumeCompulsory = Utils.createItem(Material.WRITABLE_BOOK, 1,
                Utils.title("Resume the lesson"));

        super.setItem(16 - 1, resumeCompulsory, (NetworkUser u) -> {
            // Switch to the tutorial.
            if (Event.addEvent(EventType.CONTINUE_LESSON, user.player.getUniqueId(), lessonToContinue.getLessonID(),
                    provider.tutorialsDBConnection(), log)) {
                switchServer(user);
            } else {
                user.sendMessage(net.bteuk.network.utils.Utils.error("A problem occurred, please let staff know"));
            }
        });

        // Back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1,
                Utils.title("Back"));
        super.setItem(26, back, (NetworkUser u) -> {
            user.mainGui = parentGui;
            user.mainGui.open(user.player);
            delete();
        });
    }
}
