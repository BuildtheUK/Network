package net.bteuk.network.gui.plotsystem;

import com.destroystokyo.paper.profile.PlayerProfile;
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
import org.codehaus.plexus.util.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This menu is an extension on the {@link AcceptedPlotMenu}
 * it allows the player to set a filter on the plots which show up in the menu.
 * The filter is per player, or all plots.
 */
public class FilterMenu extends NetworkRefreshableGui {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final PlotSQL plotSQL;
    private final GlobalSQL globalSQL;
    private final NetworkUser user;
    private final AcceptedPlotMenu acceptedPlotMenu;
    private int page;

    public FilterMenu(GuiProvider provider, AcceptedPlotMenu acceptedPlotMenu, NetworkUser user) {
        super(provider, 45, Component.text("Set Filter", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.acceptedPlotMenu = acceptedPlotMenu;
        this.user = user;

        this.plotSQL = provider.plotSQL();
        this.globalSQL = provider.globalSQL();

        page = 1;

        createGui();
    }

    protected void createGui() {

        // Get a list of all users that have completed plots.
        HashMap<String, Integer> map = plotSQL.getStringIntMap(
                "SELECT uuid,COUNT(id) FROM plot_review WHERE " + "accepted=1 AND completed=1 GROUP BY uuid ORDER BY COUNT(id) DESC;");
        HashMap<String, Integer> newMap = new LinkedHashMap<>();

        // The first item is for all plots.
        newMap.put("", plotSQL.getInt("SELECT COUNT(1) FROM plot_review WHERE accepted=1 AND completed=1;"));
        // Get the number for the current user and set it as the second item.
        Integer userPlots = map.get(user.player.getUniqueId().toString());
        newMap.put(user.player.getUniqueId().toString(), Objects.requireNonNullElse(userPlots, 0));
        map.remove(user.player.getUniqueId().toString());
        // Add all the other users after.
        newMap.putAll(map);

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // If the page is greater than 1, add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1, Utils.title("Previous Page"), Utils.line("Open the previous page of users.")), (NetworkUser u) ->

            {

                // Update the gui.
                page--;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // Iterate through all accepted plots.
        for (String uuid : newMap.keySet()) {

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1, Utils.title("Next Page"), Utils.line("Open the next page of users.")), (NetworkUser u) -> {

                    // Update the gui.
                    page++;
                    this.refresh();
                    this.updatePlayerInventory(u.player);
                });

                // Stop iterating.
                break;
            }

            // If skip is greater than 0, skip this iteration.
            if (skip > 0) {
                skip--;
                continue;
            }

            // The icon is the player head of the user, the amount is the number of plots they've completed.
            // If this is the current filter, make it enchanted.
            // Skip if the count is 0.
            if (newMap.get(uuid) > 0) {
                if (StringUtils.isEmpty(uuid)) {
                    setItem(slot, Utils.createItem(Material.ENDER_CHEST, newMap.get(uuid), Utils.title("All Plots"), Utils.line("Click to set the filter"),
                            Utils.line("to all completed plots.")), (NetworkUser u) -> {
                        // Set the filter and refresh the accepted plots menu at page 1.
                        acceptedPlotMenu.setFilter(uuid);
                        acceptedPlotMenu.setPage(1);
                        acceptedPlotMenu.refresh();
                        this.refresh();

                        // Return to the accepted plot menu.
                        acceptedPlotMenu.open(u.player);
                    });
                } else {

                    PlayerProfile profile = Bukkit.createProfile(UUID.fromString(uuid));
                    if (profile.hasTextures()) {
                        createPlayerHeadGuiItem(profile, newMap.get(uuid), uuid, slot);
                    } else {
                        int finalSlot = slot;
                        EXECUTOR.submit(() -> {
                            profile.complete();
                            createPlayerHeadGuiItem(profile, newMap.get(uuid), uuid, finalSlot);
                        });
                    }
                }
            }

            // Increase the slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        // Return to the plot menu.
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the accepted plot menu.")),
                (NetworkUser u) -> acceptedPlotMenu.open(u.player));
    }

    @Override
    public void delete() {
        if (acceptedPlotMenu != null) {
            acceptedPlotMenu.delete();
        }
    }

    public void deleteThis() {
        super.delete();
    }

    private void createPlayerHeadGuiItem(PlayerProfile profile, int amount, String uuid, int slot) {
        if (amount != 0) {
            setItem(slot, Utils.createPlayerSkull(profile, amount, Utils.title(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "';")),
                    Utils.line("Click to set the filter"), Utils.line("to this player.")), (NetworkUser u) -> {
                // Set the filter and refresh the accepted plots menu at page 1.
                acceptedPlotMenu.setFilter(uuid);
                acceptedPlotMenu.setPage(1);
                acceptedPlotMenu.refresh();
                this.refresh();

                // Return to the accepted plot menu.
                acceptedPlotMenu.open(u.player);
            });
        }
    }
}
