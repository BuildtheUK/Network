package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.plotsystem.ReviewFeedback;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

public class DeniedPlotFeedback extends NetworkRefreshableGui {

    private final PlotAPI plotAPI;
    private final PlotSQL plotSQL;
    private final GlobalSQL globalSQL;

    private final int plotID;

    public DeniedPlotFeedback(GuiProvider provider, int plotID) {

        super(provider, 45, Component.text("Plot " + plotID + " feedback", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.plotID = plotID;
        this.plotAPI = provider.plotAPI();
        this.plotSQL = provider.plotSQL();
        this.globalSQL = provider.globalSQL();
    }

    protected void createGui() {

        // Get the plot owner uuid.
        String uuid = plotSQL.getString("SELECT uuid FROM plot_members WHERE id=? AND is_owner=1;", plotID);

        // Get the number of times the plot was denied for the current plot owner.
        int deniedCount = plotSQL.getInt("SELECT COUNT(attempt) FROM plot_review WHERE plot_id=? AND uuid=? AND accepted=0 AND completed=1;", plotID, uuid);

        // Slot count.
        int slot = 10;

        // Iterate through the deniedCount inversely.
        // We cap the number at 21, since we'd never expect a player to have more plots denied than that,
        // it also saves us having to create multiple pages.
        for (int i = deniedCount; i > 0; i--) {

            // If the slot is greater than the number that fit in a page, stop.
            if (slot > 34) {

                break;
            }

            // Add player to gui.
            int finalI = i;
            setItem(slot, Utils.createItem(Material.WRITTEN_BOOK, 1, Utils.title("Feedback for submission " + i), Utils.line("Click to view feedback for this submission."),
                            Utils.line("Reviewed by ").append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", plotSQL.getString(
                                    "SELECT reviewer FROM plot_review WHERE plot_id=? AND uuid=? AND attempt=?;", plotID, uuid, i)),
                                    NamedTextColor.GRAY))),

                    (NetworkUser u) -> {

                        // Close the inventory.
                        u.player.closeInventory();

                        // Create the feedback book.
                        int reviewId = plotSQL.getInt("SELECT id FROM plot_review WHERE plot_id=? AND uuid=? AND attempt=?;", plotID, uuid, finalI);

                        // Open the book.
                        u.player.openBook(ReviewFeedback.createFeedbackBook(globalSQL, plotAPI, reviewId));
                    });

            // Increase the slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        // Return to the plot info menu.
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Return to the menu of plot " + plotID + ".")), (NetworkUser u) -> {

            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Switch back to plot menu.
            u.mainGui = new PlotInfo(provider, u, plotID);
            u.mainGui.open(u.player);
        });
    }
}
