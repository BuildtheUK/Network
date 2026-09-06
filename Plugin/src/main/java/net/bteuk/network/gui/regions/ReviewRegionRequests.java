package net.bteuk.network.gui.regions;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkMultiPageGui;
import net.bteuk.network.gui.staff.StaffGui;
import net.bteuk.network.regions.Request;
import net.bteuk.network.regions.sql.RegionSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class ReviewRegionRequests extends NetworkMultiPageGui {

    private final RegionSQL regionSQL;
    private final boolean staff;
    private final String uuid;
    private final Roles roles;

    private final List<Request> requests = new ArrayList<>();

    /**
     * Creates a region review menu for the player
     *
     * @param staff if it should be for staff requests
     * @param uuid  the player to create the menu for
     */
    public ReviewRegionRequests(GuiProvider provider, boolean staff, String uuid) {

        super(provider, 45, Component.text("Review Region Requests", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.roles = provider.roles();
        this.regionSQL = provider.regionSQL();

        this.staff = staff;
        this.uuid = uuid;

        setRequests();
    }

    @Override
    protected int getButtonCount() {
        return requests.size();
    }

    @Override
    protected void createPageButton(int slot, int index) {
        setItem(slot, Utils.createItem(Material.LIME_CONCRETE, 1, Utils.title("Region " + requests.get(index).region), Utils.line("Requested by ")
                        .append(Component.text(provider.globalSQL().getString("SELECT name FROM " + "player_data WHERE uuid='" + requests.get(index).uuid + "';"),
                                NamedTextColor.GRAY)),
                Utils.line("Click to open the menu for this request.")), (NetworkUser u) -> {

            // Delete this gui.
            this.delete();
            if (staff) {
                // Switch to the region request.
                u.staffGui = new ReviewRegionRequest(provider, requests.get(index), true);
                u.staffGui.open(u.player);
            } else {
                // Switch to the region request.
                u.mainGui = new ReviewRegionRequest(provider, requests.get(index), false);
                u.mainGui.open(u.player);
            }
        });
    }

    @Override
    protected void addAdditionalButtons() {
        // Return
        if (staff) {

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the staff menu.")), (NetworkUser u) -> {
                // Delete this gui.
                this.delete();

                // Switch to the staff menu.
                u.staffGui = new StaffGui(provider, u);
                u.staffGui.open(u.player);
            });
        } else {

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the region menu.")), (NetworkUser u) -> {

                // Delete this gui.
                this.delete();

                // Switch to the region menu.
                u.mainGui = new RegionMenu(provider, u);
                u.mainGui.open(u.player);
            });
        }
    }

    /**
     * Updates the list of requested reviews.
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        setRequests();
        super.refresh();
    }

    private void setRequests() {
        requests.clear();
        if (staff) {
            requests.addAll(regionSQL.getRequestList("SELECT region,uuid FROM region_requests WHERE staff_accept=0;"));
        } else {
            requests.addAll(regionSQL.getRequestList("SELECT region,uuid FROM region_requests WHERE owner_accept=0 AND " + "owner='" + uuid + "';"));
        }
    }
}
