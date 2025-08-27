package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.Network;
import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;

public class PlotMenu extends NetworkRefreshableGui {

    private final NetworkUser user;
    private final PlotSQL plotSQL;

    public PlotMenu(NetworkUser user) {

        super(45, Component.text("Plot Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        plotSQL = Network.getInstance().getPlotSQL();

        createGui();
    }

    private void createGui() {

        ArrayList<Integer> plots =
                plotSQL.getIntList("SELECT id FROM plot_members WHERE uuid='" + user.player.getUniqueId() + "' ORDER " +
                        "BY last_enter DESC;");

        // Slot count.
        int slot = 10;

        // Make a button for each plot.
        for (int i = 0; i < plots.size(); i++) {

            int finalI = i;

            // Change the colour of the material for plot owners/members.
            // Lime for owners, yellow for members.
            setItem(slot, Utils.createItem(
                            (plotSQL.hasRow(
                                    "SELECT uuid FROM plot_members WHERE uuid='" + user.player.getUniqueId() + "' AND" +
                                            " id=" + plots.get(
                                            i) + " AND is_owner=1;") ? Material.LIME_CONCRETE :
                                    Material.YELLOW_CONCRETE),
                            1,
                            Utils.title("Plot " + plots.get(i)),
                            Utils.line("Click to open the menu of this plot.")),
                    (NetworkUser u) -> {
                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch to plot info.
                        u.mainGui = new PlotInfo(u, plots.get(finalI));
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

        // Verified review menu.
        if (plotSQL.hasRow("SELECT 1 FROM plot_verification WHERE review_id IN (SELECT review_id FROM plot_review " +
                "WHERE reviewer='" + user.getUuid() + "');")) {
            setItem(4, Utils.createItem(Material.LECTERN, 1,
                            Utils.title("Verified Reviews"),
                            Utils.line("Click to view all verifications"),
                            Utils.line("on plots that you have reviewed.")),
                    (NetworkUser u) -> {
                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        u.mainGui = new VerificationMenu(u);
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
                    u.mainGui = new AcceptedPlotMenu(u);
                    u.mainGui.open(u.player);
                });

        // Return
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the building menu.")),
                (NetworkUser u) -> {
                    // Delete this gui.
                    this.delete();
                    u.mainGui = null;

                    // Switch to plot info.
                    u.mainGui = new BuildGui(u);
                    u.mainGui.open(u.player);
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }
}
