package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.api.plotsystem.VerificationStatus;
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
import java.util.List;
import java.util.Optional;

/**
 * Menu to view previous reviews that have been verified.
 */
public class VerificationMenu extends NetworkRefreshableGui {

    private final NetworkUser user;

    private final PlotSQL plotSQL;

    private ArrayList<Integer> verifications;
    private final ArrayList<Material> materials = new ArrayList<>();
    private final ArrayList<Component[]> descriptions = new ArrayList<>();

    public VerificationMenu(GuiProvider provider, NetworkUser user) {
        super(provider, 45, Component.text("Verified Review Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.plotSQL = provider.plotSQL();
    }

    @Override
    protected void loadData() {
        verifications = plotSQL.getIntList("SELECT id FROM plot_verification WHERE review_id IN " +
                "(SELECT id FROM plot_review WHERE reviewer='" + user.getUuid() + "') ORDER BY id ASC;");

        materials.clear();
        descriptions.clear();
        if (verifications != null && !verifications.isEmpty()) {
            String ids = verifications.toString().replace("[", "(").replace("]", ")");

            List<VerificationStatus> statuses = plotSQL.getVerificationStatuses(ids);

            for (int verificationId : verifications) {
                Optional<VerificationStatus> status = statuses.stream().filter(s -> s.id() == verificationId).findFirst();
                if (status.isPresent() && status.get().outcomeChanged()) {
                    materials.add(Material.RED_CONCRETE);
                    descriptions.add(new Component[]{Utils.line("The outcome of the review was altered."), Utils.line("Click" +
                            " to view the changes.")});
                } else if (status.isPresent() && status.get().selectionChanged()) {
                    materials.add(Material.ORANGE_CONCRETE);
                    descriptions.add(new Component[]{Utils.line("The selection of a category was altered."), Utils.line(
                            "Click to view the changes.")});
                } else if (status.isPresent() && status.get().feedbackChanged()) {
                    materials.add(Material.YELLOW_CONCRETE);
                    descriptions.add(new Component[]{Utils.line("The feedback of a category was altered."), Utils.line(
                            "Click to view the changes.")});
                } else {
                    materials.add(Material.LIME_CONCRETE);
                    descriptions.add(new Component[]{Utils.line("The review was not altered.")});
                }
            }
        }
    }

    protected void createGui() {

        // Slot count.
        int slot = 10;

        // Make a button for each review.
        if (this.verifications != null) {
            for (int i = 0; i < this.verifications.size(); i++) {

                int verificationId = this.verifications.get(i);
                Material item = materials.get(i);
                Component[] description = descriptions.get(i);

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
        }

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
}
