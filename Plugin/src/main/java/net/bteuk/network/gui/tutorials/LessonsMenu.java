package net.bteuk.network.gui.tutorials;

import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import teachingtutorials.tutorialobjects.LessonObject;

public class LessonsMenu extends Gui {
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
    private final Gui parentGui;

    /**
     * The list of lessons to display in this menu
     */
    private final LessonObject[] lessons;

    /**
     * The number of pages in this menu
     */
    private final int iPages;

    /**
     * The current page that the player is on
     */
    private int iPage;

    public LessonsMenu(Network plugin, NetworkUser user, Gui parentGui, LessonObject[] lessons) {
        super(54, Utils.title("Your Lessons"));
        this.plugin = plugin;
        this.user = user;
        this.parentGui = parentGui;
        this.lessons = lessons;

        this.iPages = ((lessons.length - 1) / 36) + 1;
        this.iPage = 1;

        addItems();
    }

    private void addItems() {
        // We need a page system
        // 4 lines of options
        // Blank line
        // Then arrows and back button

        // Reset page
        this.iPage = 1;

        // Indicates that there are no unfinished lessons
        if (lessons.length == 0) {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    Utils.title("You have no unfinished lessons!"));
            setItem(5 - 1, noTutorials);
        }

        // Adds the lessons
        int iStart = (iPage - 1) * 9;
        int iMax = Math.min((iPage + 3) * 9, lessons.length);
        for (int i = iStart; i < iMax; i++) {
            int finalI = i;

            ItemStack lesson = Utils.createItem(Material.WRITABLE_BOOK, 1,
                    Utils.title(lessons[i].getTutorial().getTutorialName()),
                    Utils.line(lessons[i].getLocation().getLocationName()));

            super.setItem(i - iStart, lesson, new Gui.guiAction() {
                @Override
                public void click(NetworkUser u) {
                    user.mainGui = new LessonContinueConfirmer(plugin, user, LessonsMenu.this, lessons[finalI], "Do you want to restart or resume?");
                    user.mainGui.open(user);
                }
            });
        }

        // Page back
        if (iPage > 1) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    Utils.title("Page back"));
            super.setItem(45, pageBack, new Gui.guiAction() {
                @Override
                public void click(NetworkUser u) {
                    LessonsMenu.this.iPage--;
                    refresh();
                    user.mainGui = LessonsMenu.this;
                    user.mainGui.open(user);
                }
            });
        }

        // Page forwards
        if (iPage < iPages) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback("a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44",
                    Material.ACACIA_BOAT, 1,
                    Utils.title("Page forwards"));
            super.setItem(53, pageBack, new Gui.guiAction() {
                @Override
                public void click(NetworkUser u) {
                    LessonsMenu.this.iPage++;
                    refresh();
                    user.mainGui = LessonsMenu.this;
                    user.mainGui.open(user);
                }
            });
        }

        // Back button
        ItemStack back = Utils.createItem(Material.SPRUCE_DOOR, 1,
                Utils.title("Back"));
        super.setItem(49, back, new Gui.guiAction() {
            @Override
            public void click(NetworkUser u) {
                user.mainGui = parentGui;
                user.mainGui.open(user);
                delete();
            }
        });
    }

    /**
     * Refresh the gui.
     * This usually involves clearing the content and recreating it.
     */
    @Override
    public void refresh() {
        super.clearGui();

        this.addItems();
    }
}