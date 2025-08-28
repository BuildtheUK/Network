package net.bteuk.network.gui.navigation;

import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.AddLocationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;

public class SelectSubcategory extends Gui {

    private final Network instance;
    private final AddLocation addLocation;

    private int page = 1;

    public SelectSubcategory(Network instance, AddLocation addLocation) {
        super(45, Component.text("Select Subcategory", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.instance = instance;
        this.addLocation = addLocation;
        createGui();
    }

    private void createGui() {

        // Iterate through subcatories, starting with 'None'.
        List<String> subcategories = instance.getGlobalSQL().getStringList("SELECT name FROM " +
                "location_subcategory WHERE category='" + addLocation.getCategory() + "' ORDER BY name ASC;");
        subcategories.addFirst("None");

        // If page > 1 set number of iterations that must be skipped.
        int skip = (page - 1) * 21;

        // Slot count.
        int slot = 10;

        // If page is greater than 1 add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1,
                            Utils.title("Previous Page"),
                            Utils.line("Open the previous page of subcategories.")),
                    (NetworkUser u) ->

                    {
                        // Update the gui.
                        page--;
                        this.refresh();
                        this.updatePlayerInventory(u.player);
                    });
        }

        for (String subcategory : subcategories) {

            // Skip iterations if skip > 0.
            if (skip > 0) {
                skip--;
                continue;
            }

            setItem(slot, Utils.createItem(Material.LIME_CONCRETE, 1,
                            Utils.title(subcategory),
                            Utils.line("Click to select this subcategory.")),

                    (NetworkUser u) -> {
                        // Set the county.
                        addLocation.setSubcategory(subcategory);
                        returnToAddLocation(u);
                    });

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1,
                                Utils.title("Next Page"),
                                Utils.line("Open the next page of locations.")),
                        (NetworkUser u) ->

                        {

                            // Update the gui.
                            page++;
                            this.refresh();
                            u.player.getOpenInventory().getTopInventory()
                                    .setContents(this.getInventory().getContents());
                        });

                // Stop iterating.
                break;
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
                        Utils.line("Open the previous menu.")),
                this::returnToAddLocation);
    }

    public void refresh() {

        this.createGui();
        createGui();
    }

    private void returnToAddLocation(NetworkUser u) {
        // Delete this gui.
        this.delete();
        addLocation.selectSubcategory = null;

        // Return to addlocation.
        if (addLocation.getType() == AddLocationType.ADD) {
            u.mainGui.refresh();
            u.mainGui.open(u.player);
        } else {
            u.staffGui.refresh();
            u.staffGui.open(u.player);
        }
    }
}
