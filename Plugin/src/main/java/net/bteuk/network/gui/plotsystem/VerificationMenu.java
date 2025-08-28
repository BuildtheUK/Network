package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;

/**
 * Menu to view previous reviews that have been verified.
 */
public class VerificationMenu extends NetworkRefreshableGui {

    private final NetworkUser user;

    private final PlotSQL plotSQL;

    public VerificationMenu(GuiProvider provider, NetworkUser user) {
        super(provider, 45, Component.text("Verified Review Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.plotSQL = provider.plotSQL();
    }

    protected void createGui() {

        ArrayList<Integer> verifications = plotSQL.getIntList("SELECT id FROM plot_verification WHERE review_id IN " +
                "(SELECT id FROM plot_review WHERE reviewer='" + user.getUuid() + "') ORDER BY id ASC;");

        // Slot count.
        int slot = 10;

        // Make a button for each review.
        for (int verificationId : verifications) {

            // Determine the colour based on what was changed in the verification.
            boolean feedbackChanged = plotSQL.hasRow("SELECT 1 FROM plot_verification_category WHERE " +
                    "verification_id=" + verificationId + " AND book_id_old <> book_id_new;");
            boolean selectionChanged = plotSQL.hasRow("SELECT 1 FROM plot_verification_category WHERE " +
                    "verification_id=" + verificationId + " AND selection_old <> selection_old;");
            boolean outcomeChanged = plotSQL.hasRow("SELECT 1 FROM plot_verification WHERE id=" + verificationId + " " +
                    "AND accepted_old <> accepted_new;");

            Material item;
            Component[] description;
            if (outcomeChanged) {
                item = Material.RED_CONCRETE;
                description = new Component[]{Utils.line("The outcome of the review was altered."), Utils.line("Click" +
                        " to view the changes.")};
            } else if (selectionChanged) {
                item = Material.ORANGE_CONCRETE;
                description = new Component[]{Utils.line("The selection of a category was altered."), Utils.line(
                        "Click to view the changes.")};
            } else if (feedbackChanged) {
                item = Material.YELLOW_CONCRETE;
                description = new Component[]{Utils.line("The feedback of a category was altered."), Utils.line(
                        "Click to view the changes.")};
            } else {
                item = Material.LIME_CONCRETE;
                description = new Component[]{Utils.line("The review was not altered.")};
            }

            setItem(slot, Utils.createItem(item, 1,
                            Utils.title("Verification " + verificationId),
                            description),
                    (NetworkUser u) -> {
                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch to plot info.
                        u.mainGui = new VerificationInfo(provider, verificationId);
                        u.mainGui.open(u.player);
                    });

            // Increase slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, basically add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }

            // Return
            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                            Utils.title("Return"),
                            Utils.line("Open the plot menu.")),
                    (NetworkUser u) -> {
                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch to plot info.
                        u.mainGui = new PlotMenu(provider, u);
                        u.mainGui.open(u.player);
                    });
        }
    }
}
