package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.api.plotsystem.PlotDifficulty;
import net.bteuk.network.gui.BuildGui;
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

public class PlotMenu extends NetworkRefreshableGui {

    private final NetworkUser user;
    private final PlotSQL plotSQL;

    private ArrayList<Integer> plots;
    private final ArrayList<Integer> difficulties = new ArrayList<>();
    private final ArrayList<Boolean> isOwners = new ArrayList<>();
    private boolean hasVerifiedReviews;

    public PlotMenu(GuiProvider provider, NetworkUser user) {

        super(provider, 45, Component.text("Plot Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.plotSQL = provider.plotSQL();
    }

    @Override
    protected void loadData() {
        plots = plotSQL.getIntList("SELECT id FROM plot_members WHERE uuid='" + user.player.getUniqueId() + "' ORDER " +
                "BY last_enter DESC;");

        // Get the difficulty and ownership for each plot.
        difficulties.clear();
        isOwners.clear();
        if (plots != null && !plots.isEmpty()) {
            String ids = plots.toString().replace("[", "(").replace("]", ")");
            List<PlotDifficulty> plotDifficulties = plotSQL.getPlotDifficulties(ids);
            List<Integer> ownerPlots = plotSQL.getIntList("SELECT id FROM plot_members WHERE uuid='" + user.player.getUniqueId() + "' AND is_owner=1 AND id IN " + ids + ";");

            for (int id : plots) {
                Optional<PlotDifficulty> plotDifficulty = plotDifficulties.stream().filter(pd -> pd.id() == id).findFirst();
                difficulties.add(plotDifficulty.map(PlotDifficulty::difficulty).orElse(0));
                isOwners.add(ownerPlots.contains(id));
            }
        }

        // Verified review menu.
        hasVerifiedReviews = plotSQL.hasRow("SELECT 1 FROM plot_verification WHERE review_id IN (SELECT id FROM plot_review " +
                "WHERE reviewer='" + user.getUuid() + "');");
    }

    protected void createGui() {

        // Slot count.
        int slot = 10;

        // Make a button for each plot.
        if (plots != null) {
            for (int i = 0; i < plots.size(); i++) {

                int finalI = i;

                // Change the colour of the material for plot owners/members.
                // Lime for owners, yellow for members.
                int difficulty = difficulties.get(i);
                boolean isOwner = isOwners.get(i);
                setItem(slot, Utils.createItem(getPlotIcon(difficulty, isOwner), 1, Utils.title("Plot " + plots.get(i)), Utils.line("Click to open the menu of this plot.")),
                        (NetworkUser u) -> {
                            // Delete this gui.
                            this.delete();
                            u.mainGui = null;

                            // Switch to plot info.
                            u.mainGui = new PlotInfo(provider, u, plots.get(finalI));
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
        }

        // Verified review menu.
        if (hasVerifiedReviews) {
            setItem(4, Utils.createItem(Material.LECTERN, 1,
                            Utils.title("Verified Reviews"),
                            Utils.line("Click to view all verifications"),
                            Utils.line("on plots that you have reviewed.")),
                    (NetworkUser u) -> {
                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        u.mainGui = new VerificationMenu(provider, u);
                        u.mainGui.open(u.player);
                    });
        }

        // Accepted plots menu.
        setItem(40, Utils.createItem(Material.CLOCK, 1,
                        Utils.title("Accepted Plots"),
                        Utils.line("Click to view your accepted plots.")),
                (NetworkUser u) -> {
                    // Delete this gui.
                    this.delete();
                    u.mainGui = null;

                    // Switch to plot info.
                    u.mainGui = new AcceptedPlotMenu(provider, u);
                    u.mainGui.open(u.player);
                });

        // Return
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the building menu.")),
                (NetworkUser u) -> {
                    // Delete this gui.
                    this.delete();

                    // Switch to plot info.
                    u.mainGui = new BuildGui(provider, u);
                    u.mainGui.open(u.player);
                });
    }

    private Material getPlotIcon(int difficulty, boolean isOwner) {
        return switch (difficulty) {
            case 1 -> isOwner ? Material.LIME_CONCRETE : Material.LIME_CONCRETE_POWDER;
            case 2 -> isOwner ? Material.YELLOW_CONCRETE : Material.YELLOW_CONCRETE_POWDER;
            case 3 -> isOwner ? Material.RED_CONCRETE : Material.RED_CONCRETE_POWDER;
            default -> Material.BARRIER;
        };
    }
}
