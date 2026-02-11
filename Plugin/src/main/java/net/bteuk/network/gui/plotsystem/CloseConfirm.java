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

public class CloseConfirm extends NetworkRefreshableGui {

    private final int id;
    private final PlotSQL plotSQL;

    public CloseConfirm(GuiProvider provider, int id) {

        super(provider, 27, Component.text("Save and Close Zone " + id, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.id = id;
        this.plotSQL = provider.plotSQL();
    }

    protected void createGui() {

        // Save and Close zone.
        setItem(13, Utils.createItem(Material.LIME_CONCRETE, 1, Utils.title("Save and Close Zone " + id), Utils.line("Saves the zone and closes it.")), (NetworkUser u) -> {

            // Delete this inventory.
            this.delete();
            u.mainGui = null;

            u.mainGui = new ZoneMenu(provider, u);
            u.player.closeInventory();

            // Add server event to delete plot or zone.
            provider.eventAPI().createEvent(u.player.getUniqueId().toString(),
                    plotSQL.getString("SELECT server FROM location_data WHERE name='" + plotSQL.getString("SELECT location FROM zones WHERE id=" + id + ";") + "';"),
                    "close zone" + " " + id);
        });

        // Return to zone menu.
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Return to the menu of Zone " + id + ".")), (NetworkUser u) ->

        {

            // Delete this inventory.
            this.delete();
            u.mainGui = null;

            // Switch back to zone info.
            u.mainGui = new ZoneInfo(provider, u, id, u.player.getUniqueId().toString());
            u.mainGui.open(u.player);
        });
    }
}
