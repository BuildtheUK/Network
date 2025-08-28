package net.bteuk.network.gui.plotsystem;

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

public class ZoneMenu extends NetworkRefreshableGui {

    private final NetworkUser user;
    private final PlotSQL plotSQL;

    public ZoneMenu(GuiProvider provider, NetworkUser user) {

        super(provider, 45, Component.text("Zone Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.plotSQL = provider.plotSQL();
    }

    protected void createGui() {

        /*
        Gui layout:

        List all zones that you are a member of, then all public zones that can be joined.

        Return button in the last slot.

         */

        // Get all zones that you are the owner of, order by is_owner, so zones you own show first.
        ArrayList<Integer> zones = plotSQL.getIntList("SELECT id FROM zones WHERE status='open';");

        // Slot count.
        int slot = 10;

        // Make a button for each plot.
        for (int i = 0; i < zones.size(); i++) {

            int finalI = i;

            // If you are the zone owner, or a member, open the zone info menu.
            // If the zone is public then join the zone by clicking.
            // If the zone is private, do nothing.
            if (plotSQL.hasRow(
                    "SELECT uuid FROM zone_members WHERE uuid='" + user.player.getUniqueId() + "' AND id=" + zones.get(
                            i) + ";")) {

                setItem(slot, Utils.createItem(
                                (plotSQL.hasRow(
                                        "SELECT uuid FROM zone_members WHERE uuid='" + user.player.getUniqueId() + "'" +
                                                " AND id=" + zones.get(
                                                i) + " AND is_owner=1;") ? Material.LIME_CONCRETE :
                                        Material.YELLOW_CONCRETE),
                                1,
                                Utils.title("Zone " + zones.get(i)),
                                Utils.line("Click to open the menu of this zone.")),
                        (NetworkUser u) -> {

                            // Delete this gui.
                            this.delete();
                            u.mainGui = null;

                            // Switch to zone info.
                            u.mainGui = new ZoneInfo(provider, u, zones.get(finalI), u.player.getUniqueId().toString());
                            u.mainGui.open(u.player);
                        });
            } else if (plotSQL.hasRow("SELECT id FROM zones WHERE id=" + zones.get(i) + " AND is_public=1;")) {

                setItem(slot, Utils.createItem(Material.LIGHT_BLUE_CONCRETE,
                                1,
                                Utils.title("Zone " + zones.get(i)),
                                Utils.line("Click to join this zone.")),
                        (NetworkUser u) -> {

                            // Add server event to join zone.
                            provider.eventAPI().createEvent(u.player.getUniqueId().toString(), "plotsystem",
                                    plotSQL.getString("SELECT server FROM location_data WHERE name='" +
                                            plotSQL.getString("SELECT location FROM zones WHERE id=" + zones.get(
                                                    finalI) + ";") + "';"),
                                    "join zone " + zones.get(finalI));

                            // Close inventory to prevent double clicking.
                            u.player.closeInventory();
                        });
            } else {

                setItem(slot, Utils.createItem(Material.BARRIER,
                        1,
                        Utils.title("Zone " + zones.get(i)),
                        Utils.line("This zone is private,"),
                        Utils.line("to join this zone you must be"),
                        Utils.line("invited by ")
                                .append(Component.text(provider.globalSQL().getString("SELECT name " +
                                        "FROM player_data WHERE uuid='" +
                                        plotSQL.getString("SELECT uuid FROM zone_members WHERE id=" + zones.get(i) +
                                                " AND is_owner=1;") + "';"), NamedTextColor.GRAY))));
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
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the building menu.")),
                (NetworkUser u) -> {

                    // Delete this gui.
                    this.delete();
                    u.mainGui = null;

                    // Switch to plot info.
                    u.mainGui = new BuildGui(provider, u);
                    u.mainGui.open(u.player);
                });
    }
}
