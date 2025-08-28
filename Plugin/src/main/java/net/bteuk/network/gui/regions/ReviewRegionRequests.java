package net.bteuk.network.gui.regions;

import net.bteuk.network.Network;
import net.bteuk.network.gui.staff.StaffGui;
import net.bteuk.network.sql.RegionSQL;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.regions.Request;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;

public class ReviewRegionRequests extends Gui {

    private final RegionSQL regionSQL;
    private final boolean staff;
    private final String uuid;
    private int page;

    /**
     * Creates a region review menu for the player
     *
     * @param staff if it should be for staff requests
     * @param uuid  the player to create the menu for
     */
    public ReviewRegionRequests(boolean staff, String uuid) {

        super(45, Component.text("Review Region Requests", NamedTextColor.AQUA, TextDecoration.BOLD));

        page = 1;

        regionSQL = Network.getInstance().regionSQL;

        this.staff = staff;
        this.uuid = uuid;

        createGui();
    }

    private void createGui() {

        // Get all regions with uuid.
        ArrayList<Request> requests;
        if (staff) {
            requests = regionSQL.getRequestList("SELECT region,uuid FROM region_requests WHERE staff_accept=0;");
        } else {
            requests = regionSQL.getRequestList("SELECT region,uuid FROM region_requests WHERE owner_accept=0 AND " +
                    "owner='" + uuid + "';");
        }

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // If page is greater than 1 add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1,
                    Utils.title("Previous Page"),
                    Utils.line("Open the previous page of region requests.")), (NetworkUser u) ->

            {

                // Update the gui.
                page--;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // Make a button for each plot.
        for (int i = 0; i < requests.size(); i++) {

            // If skip is greater than 0, skip this iteration.
            if (skip > 0) {
                skip--;
                continue;
            }

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1,
                        Utils.title("Next Page"),
                        Utils.line("Open the next page of regions requests.")), (NetworkUser u) ->

                {

                    // Update the gui.
                    page++;
                    this.refresh();
                    this.updatePlayerInventory(u.player);
                });

                // Stop iterating.
                break;
            }

            int finalI = i;
            setItem(slot, Utils.createItem(Material.LIME_CONCRETE, 1,
                    Utils.title("Region " + requests.get(i).region),
                    Utils.line("Requested by ")
                            .append(Component.text(Network.getInstance().getGlobalSQL().getString("SELECT name FROM " +
                                    "player_data WHERE uuid='" + requests.get(i).uuid + "';"), NamedTextColor.GRAY)),
                    Utils.line("Click to open the menu for this request.")), (NetworkUser u) -> {

                // Delete this gui.
                this.delete();
                if (staff) {

                    u.staffGui = null;

                    // Switch to region request.
                    u.staffGui = new ReviewRegionRequest(requests.get(finalI), true);
                    u.staffGui.open(u.player);
                } else {

                    u.mainGui = null;

                    // Switch to region request.
                    u.mainGui = new ReviewRegionRequest(requests.get(finalI), false);
                    u.mainGui.open(u.player);
                }
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
        if (staff) {

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                    Utils.title("Return"),
                    Utils.line("Open the staff menu.")), (NetworkUser u) ->

            {

                // Delete this gui.
                this.delete();
                u.staffGui = null;

                // Switch to staff menu.
                u.staffGui = new StaffGui(u);
                u.staffGui.open(u.player);
            });
        } else {

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                    Utils.title("Return"),
                    Utils.line("Open the region menu.")), (NetworkUser u) ->

            {

                // Delete this gui.
                this.delete();
                u.mainGui = null;

                // Switch to staff menu.
                u.mainGui = new RegionMenu(u);
                u.mainGui.open(u.player);
            });
        }
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }
}
