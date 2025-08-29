package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.RegionType;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

public class DeleteConfirm extends NetworkRefreshableGui {

    private final int id;
    private final RegionType regionType;
    private final PlotSQL plotSQL;

    public DeleteConfirm(GuiProvider provider, int id, RegionType regionType) {

        super(provider, 27, Component.text("Delete " + regionType.label + " " + id, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.id = id;
        this.regionType = regionType;
        this.plotSQL = provider.plotSQL();
    }

    protected void createGui() {

        // Delete plot
        setItem(13, Utils.createItem(Material.TNT, 1, Utils.title("Delete " + regionType.label + " " + id), Utils.line("Delete the " + regionType.label + " and its contents.")),
                (NetworkUser u) -> {

                    // Delete this inventory.
                    this.delete();
                    u.mainGui = null;

                    // Create plot or zone menu, so the next time you open the navigator you return to that.
                    // Then add server event to delete plot or zone.
                    if (regionType == RegionType.PLOT) {

                        u.mainGui = new PlotMenu(provider, u);
                        u.player.closeInventory();
                        u.sendMessage(ChatUtils.success("Deleting plot %s...", String.valueOf(id)));

                        // Add server event to delete plot or zone.
                        provider.eventAPI().createEvent(u.player.getUniqueId().toString(), "plotsystem", plotSQL.getString(
                                        "SELECT server FROM location_data WHERE name='" + plotSQL.getString("SELECT location FROM plot_data WHERE id=" + id + ";") + "';"),
                                "delete plot " + id);
                    } else if (regionType == RegionType.ZONE) {

                        u.mainGui = new ZoneMenu(provider, u);
                        u.player.closeInventory();
                        u.sendMessage(ChatUtils.success("Deleting zone %s...", String.valueOf(id)));

                        // Add server event to delete plot or zone.
                        provider.eventAPI().createEvent(u.player.getUniqueId().toString(), "plotsystem",
                                plotSQL.getString("SELECT server FROM location_data WHERE name='" + plotSQL.getString("SELECT location FROM zones WHERE id=" + id + ";") + "';"),
                                "delete" + " zone " + id);
                    }
                });

        // Return to plot info menu.
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Return to the menu of " + regionType.label + " " + id + ".")), (NetworkUser u) ->

        {

            // Delete this inventory.
            this.delete();
            u.mainGui = null;

            // Switch back to plot or zone info.
            if (regionType == RegionType.PLOT) {

                u.mainGui = new PlotInfo(provider, u, id);
            } else if (regionType == RegionType.ZONE) {

                u.mainGui = new ZoneInfo(provider, u, id, u.player.getUniqueId().toString());
            } else {

                u.mainGui = new BuildGui(provider, u);
            }

            u.mainGui.open(u.player);
        });
    }
}