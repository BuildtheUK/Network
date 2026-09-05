package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkMultiPageGui;
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
public class VerificationMenu extends NetworkMultiPageGui {

    private final PlotSQL plotSQL;

    private final ArrayList<Integer> verifications;

    public VerificationMenu(GuiProvider provider, NetworkUser user) {
        super(provider, 45, Component.text("Verified Review Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.plotSQL = provider.plotSQL();

        this.verifications = plotSQL.getIntList("SELECT id FROM plot_verification WHERE review_id IN " +
                "(SELECT id FROM plot_review WHERE reviewer='" + user.getUuid() + "') ORDER BY id ASC;");
    }

    @Override
    protected int getButtonCount() {
        return verifications.size();
    }

    @Override
    protected void createPageButton(int slot, int index) {
        int verificationId = verifications.get(index);

        // Determine the colour based on what was changed in the verification.
        boolean feedbackChanged = plotSQL.hasRow("SELECT 1 FROM plot_verification_category WHERE " +
                "verification_id=" + verificationId + " AND book_id_old <> book_id_new;");
        boolean selectionChanged = plotSQL.hasRow("SELECT 1 FROM plot_verification_category WHERE " +
                "verification_id=" + verificationId + " AND selection_old <> selection_new;");
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

    }

    @Override
    protected void addAdditionalButtons() {
        // Return
        addReturnToLastSlot((NetworkUser u) -> {
            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Switch to plot info.
            u.mainGui = new PlotMenu(provider, u);
            u.mainGui.open(u.player);
        }, Utils.line("Open the plot menu."));
    }
}
