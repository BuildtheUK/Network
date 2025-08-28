package net.bteuk.network.gui.staff;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.gui.navigation.AddLocation;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.AddLocationType;
import net.bteuk.network.utils.enums.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;

public class LocationRequests extends NetworkRefreshableGui {

    public LocationRequests(GuiProvider provider) {
        super(provider, 27, Component.text("Location Requests", NamedTextColor.AQUA, TextDecoration.BOLD));
    }

    protected void createGui() {

        // Get all locationRequests.
        ArrayList<String> locations = provider.globalSQL().getStringList("SELECT location FROM " + "location_requests;");

        // This gui only supports 7 requests due to limited space allocated.
        // However, since a large volume of requests is not expected at any point, this should be sufficient.
        // If the number does exceed 7, then they will just not show up until previous have been cleared.
        int slot = 10;

        for (String location : locations) {

            setItem(slot, Utils.createItem(Material.ORANGE_CONCRETE, 1, Utils.title(location), Utils.line("Click to review this location request.")), (NetworkUser u) -> {

                // Opens location request.
                this.delete();
                u.staffGui = null;

                int coordinate_id = provider.globalSQL().getInt("SELECT coordinate FROM " + "location_requests WHERE location='" + location + "';");
                Category category = Category.valueOf(provider.globalSQL().getString("SELECT " + "category FROM location_requests WHERE location='" + location + "';"));

                int subcategory_id = provider.globalSQL().getInt("SELECT subcategory FROM " + "location_requests WHERE location='" + location + "';");
                String subcategory = null;
                if (subcategory_id != 0) {
                    subcategory = provider.globalSQL().getString("SELECT name FROM " + "location_subcategory WHERE id=" + subcategory_id + ";");
                }

                u.staffGui = new AddLocation(provider, AddLocationType.REVIEW, location, coordinate_id, category, subcategory);
                u.staffGui.open(u.player);
            });

            slot++;

            if (slot > 16) {
                break;
            }
        }

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the staff menu.")), (NetworkUser u) ->

        {

            // Delete this gui.
            this.delete();
            u.staffGui = null;

            // Switch to staff menu.
            u.staffGui = new StaffGui(provider, u);
            u.staffGui.open(u.player);
        });
    }
}
