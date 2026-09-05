package net.bteuk.network.gui.navigation;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkMultiPageGui;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.papercore.WorldUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LocationMenu extends NetworkMultiPageGui {

    private final Category category;
    private final Category returnMenu;
    private final String[] extraInfo;
    private final GlobalSQL globalSQL;
    private final PlotSQL plotSQL;
    private List<NetworkLocation> locations;
    private Location l = null;

    /**
     * Create a new location menu.
     *
     * @param title      The title of the menu
     * @param u          The user that created the menu
     * @param category   The category of the menu
     * @param returnMenu (Optional) return menu
     * @param extraInfo  (Optional) extra info, for example the search term, subcategory, or just a list of locations.
     */
    public LocationMenu(GuiProvider provider, String title, NetworkUser u, Category category, Category returnMenu, String... extraInfo) {
        super(provider, 45, Component.text(title, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.category = category;
        this.returnMenu = returnMenu;
        this.extraInfo = extraInfo;

        this.globalSQL = provider.globalSQL();
        this.plotSQL = provider.plotSQL();

        // If the category is nearby get the player location.
        if (category == Category.NEARBY) {
            l = u.getLocationWithCoordinateTransform();
        }
        this.locations = getLocations();
    }

    @Override
    protected int getButtonCount() {
        return locations.size();
    }

    @Override
    protected void createPageButton(int slot, int index) {
        NetworkLocation location = locations.get(index);

        if (location.subcategory()) {
            // Create the subcategory button.
            setItem(slot, Utils.createItem(Material.GREEN_SHULKER_BOX, 1, Utils.title(location.name()), Utils.line("Click to open the menu for"),
                    Utils.line("for this subcategory.")), (NetworkUser u) -> {
                u.mainGui = new LocationMenu(provider, location.name(), u, Category.SUBCATEGORY, category, location.name());

                // Switch to the location menu.
                this.delete();
                u.mainGui.open(u.player);
            });
        } else {
            // Create location teleport button.
            setItem(slot, Utils.createItem(Material.ENDER_PEARL, 1, Utils.title(location.name()), Utils.line("Click to teleport here.")),

                    (NetworkUser u) -> {

                        // Get the coordinate id.
                        int coordinate_id = globalSQL.getInt("SELECT coordinate FROM " + "location_data WHERE location='" + location.name() + "';");

                        // Get the server of the location.
                        String server = globalSQL.getString("SELECT server FROM " + "coordinates WHERE id=" + coordinate_id + ";");

                        // If the plot is on the current server teleport them directly.
                        // Else teleport them to the correct server and them teleport them to the plot.
                        if (server.equals(provider.constants().serverName())) {

                            // Close inventory.
                            u.player.closeInventory();

                            // Get location from coordinate id.
                            Location l = globalSQL.getLocation(coordinate_id);

                            String worldName = globalSQL.getString("SELECT world FROM " + "coordinates WHERE id=" + coordinate_id + ";");

                            // Check if world is in plotsystem.
                            if (plotSQL.hasRow("SELECT name FROM location_data WHERE " + "name='" + worldName + "';")) {

                                // Add coordinate transformation.
                                l = new Location(WorldUtils.getWorld(worldName),
                                        l.getX() + plotSQL.getInt("SELECT xTransform " + "FROM location_data WHERE name='" + worldName + "';"), l.getY(),
                                        l.getZ() + plotSQL.getInt("SELECT zTransform " + "FROM location_data WHERE name='" + worldName + "';"), l.getYaw(), l.getPitch());
                            }

                            // Set the current location for /back
                            provider.previousLocationTracker().setPreviousCoordinate(u.player.getUniqueId().toString(), LocationAdapter.adapt(u.player.getLocation()));

                            u.player.teleport(l);
                            u.player.sendMessage(ChatUtils.success("Teleported to ").append(Component.text(location.name(), NamedTextColor.DARK_AQUA)));
                        } else {

                            u.player.closeInventory();

                            // Create teleport event.
                            provider.eventAPI().createTeleportEvent(true, u.player.getUniqueId().toString(), "teleport location " + location.name(),
                                    LocationAdapter.adapt(u.player.getLocation()));

                            // Switch server.
                            provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), server);
                        }
                    });
        }
    }

    @Override
    protected void addAdditionalButtons() {
        // Return (optional)
        if (returnMenu != null) {
            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the previous menu.")), (NetworkUser u) -> {
                // Delete this gui.
                this.delete();

                // Switch to navigation menu.
                NetworkRefreshableGui returnGui = getReturnGui(u);
                if (returnGui != null) {
                    u.mainGui = returnGui;
                    returnGui.open(u.player);
                } else {
                    u.player.sendMessage(ChatUtils.error("An error occurred, please contact an admin."));
                    u.player.closeInventory();
                }
            });
        }
    }

    @Override
    public void refresh() {
        // Refresh the location list.
        locations = getLocations();
        super.refresh();
    }

    public boolean isEmpty() {
        return locations.isEmpty();
    }

    // Method to determine the search parameters when getting the locations to display in the menu.
    private List<NetworkLocation> getLocations() {

        // We use a linked hashmap to preserve insertion order. Since in most cases we want subcategories to be
        // listed first.
        List<NetworkLocation> locations = new ArrayList<>();

        switch (category) {

            // Main categories (can include subcategories.
            case ENGLAND, SCOTLAND, WALES, NORTHERN_IRELAND, OTHER -> {
                globalSQL.getStringList("SELECT name FROM location_subcategory WHERE " + "category='" + category + "' ORDER BY name ASC;")
                        .forEach(name -> locations.add(new NetworkLocation(name, true)));
                globalSQL.getStringList("SELECT location FROM location_data WHERE " + "category='" + category + "' AND subcategory is null ORDER BY location ASC;")
                        .forEach(name -> locations.add(new NetworkLocation(name, false)));
            }
            // Subcategory, can only include locations.
            case SUBCATEGORY -> {
                // Get the subcategory id from the name.
                int id = globalSQL.getInt("SELECT id FROM location_subcategory WHERE " + "name='" + extraInfo[0] + "';");
                globalSQL.getStringList("SELECT location FROM location_data WHERE " + "subcategory=" + id + " ORDER BY location ASC;").forEach(name -> locations.add(new NetworkLocation(name, false)));
            }

            // Suggested locations can only include locations.
            case SUGGESTED ->
                    globalSQL.getStringList("SELECT location FROM location_data WHERE " + "suggested=1 ORDER BY location ASC;").forEach(name -> locations.add(new NetworkLocation(name, false)));

            // Nearby locations can only include locations and are found based on the player's current location.
            case NEARBY -> getNearbyLocations().forEach(name -> locations.add(new NetworkLocation(name, false)));

            // Search locations based on the given string query.
            case SEARCH -> searchLocations().forEach(name -> locations.add(new NetworkLocation(name, false)));

            // Temporary implies that the menu is being opened from the map.
            // A temporary menu provides the list of locations as extra args.
            case TEMPORARY -> {
                for (String value : extraInfo) {
                    locations.add(new NetworkLocation(value, false));
                }
            }
        }
        return locations;
    }

    private LinkedHashSet<String> getNearbyLocations() {
        return new LinkedHashSet<>(globalSQL.getStringList(
                "SELECT location_data.location " + "FROM location_data INNER JOIN coordinates ON location_data.coordinate=coordinates.id " + "WHERE ((((coordinates.x/1000)-" + (l.getX() / 1000) + ")*((coordinates.x/1000)-" + (l.getX() / 1000) + ")) + " + "(((coordinates.z/1000)-" + (l.getZ() / 1000) + ")*((coordinates.z/1000)-" + (l.getZ() / 1000) + ")))" + " < " + (provider.constants()
                        .navigationRadius() * provider.constants()
                        .navigationRadius()) + " ORDER BY ((((coordinates.x/1000)-" + (l.getX() / 1000) + ")*((coordinates.x/1000)-" + (l.getX() / 1000) + ")) + " + "((" +
                        "(coordinates.z/1000)-" + (l.getZ() / 1000) + ")*((coordinates.z/1000)-" + (l.getZ() / 1000) + ")))" + " ASC;"));
    }

    private Set<String> searchLocations() {

        if (extraInfo == null || extraInfo[0] == null) {
            return Collections.emptySet();
        }

        // The search query is the first argument of the extra info.
        String[] parts = extraInfo[0].trim().split(" ");
        String[] queryParts = new String[parts.length];
        String[] whereParts = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            queryParts[i] = "%" + parts[i] + "%";
            whereParts[i] = "LIKE ?";
        }
        String whereLocation = String.join(" AND location ", whereParts);
        String whereCategory = String.join(" AND category ", whereParts);
        String whereSubcategory = String.join(" AND s.name ", whereParts);

        // Search for locations that include the phrase.
        ArrayList<String> locations = globalSQL.getStringList("SELECT location FROM location_data WHERE location " + whereLocation + " ORDER BY location ASC;", queryParts);

        // Also search for any categories or subcategories.
        locations.addAll(globalSQL.getStringList("SELECT location FROM location_data WHERE category " + whereCategory + " ORDER BY location ASC;", queryParts));
        locations.addAll(globalSQL.getStringList("SELECT l.location FROM location_data l INNER JOIN location_subcategory s ON l.subcategory=s.id WHERE s.name " + whereSubcategory + " ORDER BY l.location ASC;", queryParts));

        return new LinkedHashSet<>(locations);
    }

    // Function to get the gui for the return button.
    private NetworkRefreshableGui getReturnGui(NetworkUser u) {
        if (returnMenu == Category.EXPLORE) {
            return new ExploreGui(provider, u);
        } else if (returnMenu == Category.TEMPORARY) {
            // If the returnMenu is temporary, it implies that a subcategory was opened from a temporary menu.
            // In this case the locations to add to the menu will in the extra info, excluding the first value, which
            // is the subcategory.
            if (extraInfo.length > 1) {
                LocationMenu gui = new LocationMenu(provider, "Map", u, Category.TEMPORARY, null, Arrays.copyOfRange(extraInfo, 1, extraInfo.length));
                gui.setDeleteOnClose(true);
                u.mainGui = gui;
                return gui;
            } else {
                return null;
            }
        } else {
            return new LocationMenu(provider, returnMenu.getLabel(), u, returnMenu, Category.EXPLORE);
        }
    }

    private record NetworkLocation(String name, boolean subcategory) {
    }
}
