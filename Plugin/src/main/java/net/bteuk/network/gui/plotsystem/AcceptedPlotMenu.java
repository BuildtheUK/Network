package net.bteuk.network.gui.plotsystem;

import com.destroystokyo.paper.profile.PlayerProfile;
import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This Gui class allows the player to view accepted plots.
 * Default is filtered to view the completed plots by the user.
 * However, the filter can be altered to view all accepted plots,
 * or those by a specific user, granted they have completed at least one plot.
 */
public class AcceptedPlotMenu extends NetworkRefreshableGui {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final PlotSQL plotSQL;
    private final GlobalSQL globalSQL;

    private final FilterMenu filterMenu;

    @Setter
    private PlotInfo plotInfo;

    /**
     * Filter that is applied when fetching all plots.
     * The uuid is the value.
     * An empty string implies all users.
     */
    @Getter
    @Setter
    private String filter;

    @Setter
    private int page = 1;

    public AcceptedPlotMenu(GuiProvider provider, NetworkUser user) {

        super(provider, 45, Component.text("Accepted Plot Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        filter = user.player.getUniqueId().toString();
        this.filterMenu = new FilterMenu(provider, this, user);

        this.plotSQL = provider.plotSQL();
        this.globalSQL = provider.globalSQL();

        createGui();
    }

    protected void createGui() {

        // Fetch accepted plots.
        HashMap<Integer, String> plots;
        if (StringUtils.isEmpty(filter)) {
            plots = plotSQL.getIntStringMap("SELECT plot_id,uuid FROM plot_review WHERE accepted=1 AND completed=1 " + "ORDER BY review_time DESC;");
        } else {
            plots = plotSQL.getIntStringMap("SELECT plot_id,uuid FROM plot_review WHERE accepted=1 AND completed=1 " + "AND uuid='" + filter + "' ORDER BY review_time DESC;");
        }

        // Set the filter.
        // Open the filter menu.
        setItem(4, Utils.createItem(Material.SPRUCE_SIGN, 1, Utils.title("Set filter"), Utils.line("The current filter is set to: ")
                .append(Component.text(StringUtils.isEmpty(filter) ? "All Players" : globalSQL.getString("SELECT name FROM " + "player_data WHERE uuid='" + filter + "';"),
                        NamedTextColor.GRAY)), Utils.line("Click to select a different filter.")), (NetworkUser u) -> filterMenu.open(u.player));

        // Slot count.
        int slot = 10;

        // If page > 1 set number of iterations that must be skipped.
        int skip = (page - 1) * 21;

        // If page is greater than 1 add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1, Utils.title("Previous Page"), Utils.line("Open the previous page of accepted plots.")), (NetworkUser u) -> {
                // Update the gui.
                page--;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // Make a button for each plot.
        for (int plotID : plots.keySet()) {

            // Skip iterations if skip > 0.
            if (skip > 0) {
                skip--;
                continue;
            }

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1, Utils.title("Next Page"), Utils.line("Open the next page of accepted plots.")), (NetworkUser u) ->

                {
                    // Update the gui.
                    page++;
                    this.refresh();
                    this.updatePlayerInventory(u.player);
                });

                // Stop iterating.
                break;
            }

            // The icon is the player head of the plot builder.
            // If the texture is not available, load the item async.
            PlayerProfile profile = Bukkit.createProfile(UUID.fromString(plots.get(plotID)));
            if (profile.hasTextures()) {
                createPlayerHeadGuiItem(profile, plotID, plots.get(plotID), slot);
            } else {
                int finalSlot = slot;
                EXECUTOR.submit(() -> {
                    profile.complete();
                    createPlayerHeadGuiItem(profile, plotID, plots.get(plotID), finalSlot);
                });
            }

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
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the plot menu.")), (NetworkUser u) -> {
            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Return to the plot menu.
            u.mainGui = new PlotMenu(provider, u);
            u.mainGui.open(u.player);
        });
    }

    @Override
    public void delete() {
        if (filterMenu != null) {
            filterMenu.deleteThis();
        }
        if (plotInfo != null) {
            plotInfo.deleteThis();
        }
        super.delete();
    }

    private void createPlayerHeadGuiItem(PlayerProfile profile, int plotID, String uuid, int slot) {
        ItemStack guiItem = Utils.createPlayerSkull(profile, 1, Utils.title("Plot " + plotID),
                Utils.line("Completed by: ").append(Component.text(globalSQL.getString("SELECT name FROM player_data " + "WHERE uuid='" + uuid + "';"), NamedTextColor.GRAY)),
                Utils.line("Click to open the menu of this plot."));

        setItem(slot, guiItem, (NetworkUser u) -> {
            // Switch to plot info.
            if (plotInfo != null) {
                plotInfo.deleteThis();
            }
            plotInfo = new PlotInfo(provider, u, plotID);
            plotInfo.setAcceptedPlotMenu(this);
            plotInfo.open(u.player);
        });
    }
}
