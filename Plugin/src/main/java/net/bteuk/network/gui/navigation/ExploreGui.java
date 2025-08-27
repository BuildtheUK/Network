package net.bteuk.network.gui.navigation;

import net.bteuk.network.Network;
import net.bteuk.network.eventing.listeners.navigation.LocationSearch;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.AddLocationType;
import net.bteuk.network.utils.enums.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import static net.bteuk.network.utils.NetworkConfig.CONFIG;

public class ExploreGui extends NetworkRefreshableGui {

    private final NetworkUser u;

    public ExploreGui(NetworkUser u) {

        super(27, Component.text("Exploration Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.u = u;

        createGui();
    }

    private void createGui() {

        // If the player has the correct permission allow them to request a location.
        if (u.player.hasPermission("uknet.navigation.request")) {

            setItem(18, Utils.createItem(Material.MAGENTA_GLAZED_TERRACOTTA, 1, Utils.title("Add Location"),
                    Utils.line("Request a new location to add"), Utils.line("to the exploration menu.")), (NetworkUser u) -> {

                this.delete();
                u.mainGui = null;

                // Switch to the location add menu.
                u.mainGui = new AddLocation(AddLocationType.ADD);
                u.mainGui.open(u.player);
            });
        }

        /*
        Create a button for each main category.

        The main categories are:

        - England
        - Scotland
        - Wales
        - Northern Ireland
        - Other

        - Suggested Locations
        - Nearby Locations
        - Find Location

         */

        // England
        setItem(2, Utils.createCustomSkullWithFallback("bee5c850afbb7d8843265a146211ac9c615f733dcc5a8e2190e5c247dea32",
                Material.ORANGE_CONCRETE_POWDER, 1, Utils.title("England"), Utils.line("Click to pick from"),
                Utils.line("locations in England.")), u -> openLocation("England", u, Category.ENGLAND));

        // Scotland
        setItem(3,
                Utils.createCustomSkullWithFallback("dadc377816389c3c87c65dcacac1d8f880b54334d7c23ea22f099e2c4eab1ff9",
                        Material.LIGHT_BLUE_CONCRETE_POWDER, 1, Utils.title("Scotland"),
                        Utils.line("Click to pick from"), Utils.line("locations in Scotland.")),
                u -> openLocation("Scotland", u, Category.SCOTLAND));

        // Wales
        setItem(4,
                Utils.createCustomSkullWithFallback("8140ad08f7ee1c73bf75660614595c7392caba5529211a9adbe3b5639cb6ad41",
                        Material.RED_CONCRETE_POWDER, 1, Utils.title("Wales"), Utils.line("Click to pick from"),
                        Utils.line("locations in Wales.")), u -> openLocation("Wales", u, Category.WALES));

        // Northern Ireland
        setItem(5,
                Utils.createCustomSkullWithFallback("c00ae311a5c7082e76450ecafcbbbc07dcdc484600ac0bf8d91f27e0a65b7e32",
                        Material.LIME_CONCRETE_POWDER, 1, Utils.title("Northern Ireland"),
                        Utils.line("Click to pick from"), Utils.line("locations in Norther " + "Ireland.")),
                u -> openLocation("Northern Ireland", u, Category.NORTHERN_IRELAND));

        // Other
        setItem(6,
                Utils.createCustomSkullWithFallback("c439d7f9c67f32dcbb86b7010b1e14b60de96776a35f61cee982660aacf5264b",
                        Material.YELLOW_CONCRETE_POWDER, 1, Utils.title("Other"),
                        Utils.line("Click to pick from locations"),
                        Utils.line("not in the 4 " + "countries of the UK.")),
                u -> openLocation("Other", u, Category.OTHER));

        // Suggested Locations
        // Gets all locations which have suggested=1 in database.
        setItem(21, Utils.createItem(Material.GOLD_BLOCK, 1, Utils.title("Suggested Locations"),
                        Utils.line("Click " + "to" + " view locations"), Utils.line("that are recommended to view.")),
                u -> openLocation("Suggested" + " " + "Locations", u, Category.SUGGESTED));

        // Nearby Locations (radius set in config under navigation_radius)
        setItem(22, Utils.createItem(Material.COMPASS, 1, Utils.title("Nearby Locations"),
                        Utils.line("Click to view " + "locations"),
                        Utils.line("in a " + CONFIG.getInt("navigation_radius") + "km radius.")),
                u -> openLocation("Nearby Locations", u, Category.NEARBY));

        // Find Locations
        setItem(23, Utils.createItem(Material.OAK_SIGN, 1, Utils.title("Find Locations"),
                Utils.line("Click to " + "search" + " for locations"), Utils.line("based on chat input.")), (NetworkUser u) -> {
            u.player.sendMessage(ChatUtils.success("Type a word or phrase in chat to search for locations."));
            new LocationSearch(u);
            u.player.closeInventory();
        });

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"),
                Utils.line("Open the navigator " + "main menu.")), (NetworkUser u) -> {
            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Switch to navigation menu.
            Network.getInstance().navigatorGui.open(u.player);
        });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }

    private void openLocation(String name, NetworkUser u, Category category) {

        LocationMenu gui = new LocationMenu(name, u, category, Category.EXPLORE);

        if (gui.isEmpty()) {

            gui.delete();
            u.player.sendMessage(ChatUtils.error("No locations added to the menu in ")
                    .append(Component.text(name, NamedTextColor.DARK_RED)));
        } else {

            // Switch to location menu.
            this.delete();
            u.mainGui = gui;
            u.mainGui.open(u.player);
        }
    }
}