package net.bteuk.network.gui.tutorials;

import lombok.extern.java.Log;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.texteditorbooks.BookCloseAction;
import net.bteuk.network.utils.texteditorbooks.TextEditorBookListener;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import teachingtutorials.tutorialobjects.Tutorial;
import teachingtutorials.tutorialobjects.TutorialRecommendation;
import teachingtutorials.utils.Display;

import java.util.UUID;

@Log
public class RecommendationAddGui extends NetworkRefreshableGui {
    private final NetworkRefreshableGui parentGui;

    private final int iPlotID;

    net.bteuk.network.utils.TutorialRecommendation[] currentRecommendations;

    /**
     * The TutorialID of the selected tutorial
     */
    private int iTutorialID;

    private final NetworkUser user;

    private final UUID ownerUUID;

    private final TextEditorBookListener reasonEditor;

    private final Tutorial[] allTutorials;

    /**
     * The number of pages in this menu
     */
    private final int iPages;

    /**
     * The current page that the player is on
     */
    private int iPage;

    public RecommendationAddGui(GuiProvider provider, NetworkRefreshableGui parentGui, NetworkUser user, int iPlotID, UUID ownerUUID,
                                net.bteuk.network.utils.TutorialRecommendation[] currentRecommendations) {
        super(provider, 54, Utils.title("Tutorial Recommendation"));
        this.parentGui = parentGui;
        this.user = user;
        this.iPlotID = iPlotID;
        this.ownerUUID = ownerUUID;
        this.currentRecommendations = currentRecommendations;

        // Fetches all in use tutorials
        allTutorials = Tutorial.fetchAll(true, false, null, provider.tutorialsDBConnection(), log);

        this.iPages = ((allTutorials.length - 1) / 45) + 1;
        this.iPage = 1;

        reasonEditor = new TextEditorBookListener(provider.instance(), user, this, "Reason Editor",
                new BookCloseAction() {
                    @Override
                    public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener textEditorBookListener, String szNewContent) {
                        // Unregister the book
                        textEditorBookListener.unregister();

                        // Always remove the book
                        user.player.getInventory().getItemInMainHand().setAmount(0);

                        // Check that it is the correct length etc
                        if (szNewContent.length() > 100) {
                            user.player.sendMessage(Display.errorText("The reason must not be more than 100 characters"));
                            return false;
                        } else {
                            return true;
                        }
                    }

                    @Override
                    public void runPostClose() {
                        open(user.player);
                    }
                }
                , "");

        createGui();
    }

    protected void createGui() {
        // Back button
        setItem(53, Utils.createItem(Material.BARRIER, 1,
                        ChatUtils.title("Delete"),
                        ChatUtils.line("Go back to the recommended tutorials.")),

                (NetworkUser u) -> {
                    // Go back to the review gui.
                    this.delete();
                    parentGui.open(u.player);
                }
        );

        // Confirm button - if a tutorial has been chosen
        if (iTutorialID > 0) {
            setItem(45, Utils.createItem(Material.EMERALD, 1,
                            ChatUtils.title("Submit"),
                            ChatUtils.line("Add recommendation.")),

                    (NetworkUser u) -> {

                        boolean bTutorialAlreadyRecommendedAndNotComplete = false;

                        for (net.bteuk.network.utils.TutorialRecommendation tutorialRecommendation : currentRecommendations) {
                            if (tutorialRecommendation.getLinkedTutorialRecommendation().getTutorialID() == iTutorialID && !tutorialRecommendation.getLinkedTutorialRecommendation()
                                    .isCompleted()) {
                                bTutorialAlreadyRecommendedAndNotComplete = true;
                                break;
                            }
                        }

                        if (bTutorialAlreadyRecommendedAndNotComplete) {
                            user.player.sendMessage(ChatUtils.error("This tutorial is already recommended and has not been completed"));
                        } else {
                            int iRecommendationID = TutorialRecommendation.addRecommendation(provider.tutorialsDBConnection(), log,
                                    ownerUUID, user.player.getUniqueId(), iTutorialID, -1,
                                    ((TextComponent) ((BookMeta) reasonEditor.getBook().getItemMeta()).page(1)).content());

                            // Adds recommendation to the plot system DB
                            net.bteuk.network.utils.TutorialRecommendation tutorialRecommendation = new net.bteuk.network.utils.TutorialRecommendation(provider.tutorialsDBConnection(), iRecommendationID, iPlotID);
                            tutorialRecommendation.addTutorialRecommendationToDB(provider.plotSQL());

                            // Go back to the recommended tutorials list.
                            u.player.closeInventory();
                            parentGui.refresh();
                            parentGui.open(u.player);
                            delete();
                        }

                        // Adds recommendation to the tutorial DB
                    });
        }

        // Reason button
        setItem(50, Utils.createItem(Material.WRITABLE_BOOK, 1,
                ChatUtils.title("Set Reason")), (NetworkUser u) -> {
            reasonEditor.startEdit("Reason Editor");
        });

        // Indicates that there are no tutorials
        if (allTutorials.length == 0) {
            ItemStack noTutorials = Utils.createItem(Material.BARRIER, 1,
                    Utils.title("There are no tutorials to choose from"));
            setItem(5 - 1, noTutorials);
        }

        // Page back
        if (iPage > 1) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback(provider.instance(), "4eff72715e6032e90f50a38f4892529493c9f555b9af0d5e77a6fa5cddff3cd2",
                    Material.ACACIA_BOAT, 1,
                    Utils.title("Page back"));
            super.setItem(46, pageBack, (NetworkUser u) -> {
                iPage--;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // Page forwards
        if (iPage < iPages) {
            ItemStack pageBack = Utils.createCustomSkullWithFallback(provider.instance(), "a7ba2aa14ae5b0b65573dc4971d3524e92a61dd779e4412e4642adabc2e56c44",
                    Material.ACACIA_BOAT, 1,
                    Utils.title("Page forwards"));
            super.setItem(52, pageBack, (NetworkUser u) -> {
                iPage++;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // Adds the tutorials
        int iStart = (iPage - 1) * 9;
        int iMax = Math.min((iPage + 4) * 9, allTutorials.length);
        int i;
        for (i = iStart; i < iMax; i++) {
            int finalI = i;

            Material material;

            if (allTutorials[i].getTutorialID() == iTutorialID)
                material = Material.KNOWLEDGE_BOOK;
            else
                material = Material.BOOK;

            ItemStack tutorial = teachingtutorials.utils.Utils.createItem(material, 1,
                    Utils.title(allTutorials[i].getTutorialName()),
                    Utils.line("By " + Bukkit.getOfflinePlayer(allTutorials[i].getUUIDOfAuthor()).getName()));

            super.setItem(i - iStart, tutorial, (NetworkUser u) -> {
                iTutorialID = allTutorials[finalI].getTutorialID();
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }
    }
}
