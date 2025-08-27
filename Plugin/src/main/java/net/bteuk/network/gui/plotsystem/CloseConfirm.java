package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.Network;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

public class CloseConfirm extends Gui {

    private final int id;

    public CloseConfirm(int id) {

        super(27, Component.text("Save and Close Zone " + id, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.id = id;

        createGui();
    }

    private void createGui() {

        // Get plot sql.
        PlotSQL plotSQL = Network.getInstance().getPlotSQL();

        // Save and Close zone.
        setItem(13, Utils.createItem(Material.LIME_CONCRETE, 1,
                        Utils.title("Save and Close Zone " + id),
                        Utils.line("Saves the zone and closes it.")),
                (NetworkUser u) -> {

                    // Delete this inventory.
                    this.delete();
                    u.mainGui = null;

                    u.mainGui = new ZoneMenu(u);
                    u.player.closeInventory();

                    // Add server event to delete plot or zone.
                    EventManager.createEvent(u.player.getUniqueId().toString(), "plotsystem", plotSQL.getString(
                                    "SELECT server FROM location_data WHERE name='" +
                                            plotSQL.getString("SELECT location FROM zones WHERE id=" + id + ";") +
                                            "';"),
                            "close zone" +
                                    " " + id);
                });

        // Return to zone menu.
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Return to the menu of Zone " + id + ".")),
                u ->

                {

                    // Delete this inventory.
                    this.delete();
                    u.mainGui = null;

                    // Switch back to zone info.
                    u.mainGui = new ZoneInfo(u, id, u.player.getUniqueId().toString());
                    u.mainGui.open(u.player);
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }
}
