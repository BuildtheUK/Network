package net.bteuk.network.lobby;

import eu.decentsoftware.holograms.api.holograms.Hologram;
import io.papermc.lib.PaperLib;
import lombok.extern.java.Log;
import net.bteuk.network.CommandManager;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.core.Constants;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.navigation.LocationMenu;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.utils.Holograms;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.enums.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * This class manages the ingame map in the lobby.
 * When a player enters the map area they will have an item in the 5th slot of their hotbar.
 * Clicking on the item will list all nearby locations (radius of 50km) to the players position.
 * Locations will be sorted by distance to the player.
 */
@Log
public class Map extends AbstractReloadableComponent {

    private static final String AQUA = "&b&l";

    private static final String GOLD = "&6&l";

    private final Network instance;
    private final Constants constants;
    private final ServerAPI serverAPI;
    private final EventAPI eventAPI;
    private final GuiProvider guiProvider;

    /**
     * The server that has the physical map.
     */
    private String server;

    /**
     * Coordinates of the map.
     */
    private Location mapLocation;

    /**
     * HashMap of the holograms
     * Key: Hologram
     * Value: Click action for the hologram
     */
    private HashMap<Hologram, HologramClickAction> holograms;

    private HologramClickEvent hologramClickEvent;

    public Map(Network instance, Constants constants, ServerAPI serverAPI, EventAPI eventAPI, GuiProvider guiProvider) {
        this.instance = instance;
        this.constants = constants;
        this.serverAPI = serverAPI;
        this.eventAPI = eventAPI;
        this.guiProvider = guiProvider;
    }

    private static List<String> appendColour(List<String> lines, boolean subcategory) {
        List<String> newList = new ArrayList<>();
        lines.forEach(line -> {
            if (subcategory) {
                newList.add(GOLD + line);
            } else {
                newList.add(AQUA + line);
            }
        });
        return newList;
    }

    /**
     * Loads the map using the config from map.yml
     * If there are issues with the config, then loading will be unsuccessful and the map will not be enabled.
     */
    @Override
    public void load() {
        if (isEnabled()) {
            log.warning("An attempt was made to load the Map while it is already enabled.");
            return;
        }

        // Check if the map is enabled.
        if (!constants.mapEnabled()) {
            setEnabled(false);
            return;
        }

        // Get the server of the map, this is important in deciding which features to enable.
        server = constants.mapServer();
        if (server == null || !instance.getGlobalSQL().hasRow("SELECT * FROM server_data WHERE name='" + server + "';")) {
            setEnabled(false);
            log.warning("The map has been enabled without a valid server, disabling the map.");
            return;
        }

        // Set the location of the map.
        // If the map is on this server the world must exist.
        // Set the coordinates of the location first, the server is not relevant for this part.
        mapLocation = new Location(null, constants.mapLocation().x(), constants.mapLocation().y(), constants.mapLocation().z(), constants.mapLocation().yaw(),
                constants.mapLocation().pitch());
        if (Objects.equals(constants.serverName(), server)) {
            if (constants.mapLocation().world() == null || Bukkit.getWorld(constants.mapLocation().world()) == null) {
                setEnabled(false);
                log.warning("The map world does not exist on this server, disabling the map.");
                return;
            }
            // Set the world, the coordinates have already been set.
            mapLocation.setWorld(Bukkit.getWorld(constants.mapLocation().world()));

            // Register the hologram click event.
            hologramClickEvent = new HologramClickEvent(instance, this);

            // Load the map markers and linked location.
            try {
                loadMarkers();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        setEnabled(true);
    }

    public void registerMapCommand(CommandManager manager) {
        // Enable the map command.
        manager.registerCommand(new MapCommand(this, server, constants, guiProvider.globalSQL()));
    }

    /**
     * Unloads the map if currently enabled.
     */
    @Override
    public void unload() {
        if (isEnabled()) {
            // Remove all visible markers.
            holograms.forEach((hologram, clickAction) -> hologram.delete());
            holograms.clear();

            // Disable the hologram click event.
            hologramClickEvent.unregister();
            hologramClickEvent = null;
            setEnabled(false);
        }
    }

    public HologramClickAction getHologramClickAction(Hologram hologram) {
        return holograms.get(hologram);
    }

    protected void teleport(Player p) {
        // If the map is on this server teleport the player directly, else switch server first.
        if (Objects.equals(constants.serverName(), server)) {
            PaperLib.teleportAsync(p, mapLocation);
        } else {
            // Create teleport event.
            eventAPI.createTeleportEvent(true, p.getUniqueId().toString(),
                    String.format("teleport %s " + "%f %f %f %f %f", constants.mapLocation().world(), mapLocation.getX(), mapLocation.getY(), mapLocation.getZ(),
                            mapLocation.getYaw(), mapLocation.getPitch()), "&aTeleporting to the map.", LocationAdapter.adapt(p.getLocation()));

            // Switch server.
            serverAPI.switchServer(PlayerAdapter.adapt(p), server);
        }
    }

    /**
     * Add a new marker to the map.
     *
     * @param l      location where the marker should be placed
     * @param marker the name of the marker, must be the name of a location or subcategory
     * @return feedback for the player
     */
    protected Component addMarker(Location l, String marker) {

        // Adjust the location to have the correct y.
        Location marker_location = l.clone();
        marker_location.setY(l.getY() + 0.5);

        // Check of the name is valid.
        if (instance.getGlobalSQL().hasRow("SELECT location FROM location_data WHERE location='" + marker + "';")) {
            // Check if marker does not already exist.
            if (instance.getGlobalSQL().hasRow("SELECT location FROM location_marker WHERE location='" + marker + "';")) {
                return Component.text(marker, NamedTextColor.DARK_RED).append(ChatUtils.error(" already exists on the" + " map."));
            }
            // Create coordinate id.
            int coordinate_id = instance.getGlobalSQL().addCoordinate(marker_location);
            // Add marker.
            instance.getGlobalSQL().update("INSERT INTO location_marker(location,coordinate_id) VALUES('" + marker + "'," + coordinate_id + ");");
            reload();
            return ChatUtils.success("Added marker for location ").append(Component.text(marker, NamedTextColor.DARK_AQUA));
        } else {
            // Get the subcategory id.
            int subcategory_id = instance.getGlobalSQL().getInt("SELECT id FROM location_subcategory WHERE name='" + marker + "';");
            if (subcategory_id == 0) {
                return Component.text(marker, NamedTextColor.DARK_RED).append(ChatUtils.error(" is not a valid " + "location or subcategory name."));
            }
            // Check if marker does not already exist.
            if (instance.getGlobalSQL().hasRow("SELECT subcategory FROM location_marker WHERE subcategory='" + subcategory_id + "';")) {
                return Component.text(marker, NamedTextColor.DARK_RED).append(ChatUtils.error(" already exists on the" + " map."));
            }
            // Create coordinate id.
            int coordinate_id = instance.getGlobalSQL().addCoordinate(marker_location);
            // Add marker.
            instance.getGlobalSQL().update("INSERT INTO location_marker(subcategory,coordinate_id) VALUES('" + subcategory_id + "'," + coordinate_id + ");");
            reload();
            return ChatUtils.success("Added marker for subcategory ").append(Component.text(marker, NamedTextColor.DARK_AQUA));
        }
    }

    protected Component removeMarker(String marker) {
        // Check if the marker is a valid location.
        if (instance.getGlobalSQL().hasRow("SELECT location FROM location_marker WHERE location='" + marker + "';")) {
            // Remove coordinate id.
            int coordinate_id = instance.getGlobalSQL().getInt("SELECT coordinate_id FROM location_marker WHERE " + "location='" + marker + "';");
            // Remove marker of location.
            instance.getGlobalSQL().update("DELETE FROM location_marker WHERE location='" + marker + "';");
            instance.getGlobalSQL().update("DELETE FROM coordinates WHERE id=" + coordinate_id);
            reload();
            return ChatUtils.success("Removed marker for location ").append(Component.text(marker, NamedTextColor.DARK_AQUA));
        } else {
            // Else check if it's a valid subcategory.
            int subcategory_id = instance.getGlobalSQL().getInt("SELECT id FROM location_subcategory WHERE name='" + marker + "';");
            if (subcategory_id == 0) {
                return Component.text(marker, NamedTextColor.DARK_RED).append(ChatUtils.error(" is not a valid marker" + "."));
            }
            // Remove coordinate id.
            int coordinate_id = instance.getGlobalSQL().getInt("SELECT coordinate_id FROM location_marker WHERE " + "subcategory=" + subcategory_id + ";");
            // Remove marker of location.
            instance.getGlobalSQL().update("DELETE FROM location_marker WHERE subcategory=" + subcategory_id + ";");
            instance.getGlobalSQL().update("DELETE FROM coordinates WHERE id=" + coordinate_id);
            reload();
            return ChatUtils.success("Removed marker for subcategory ").append(Component.text(marker, NamedTextColor.DARK_AQUA));
        }
    }

    private void loadMarkers() throws SQLException {

        // Create the holograms map.
        holograms = new HashMap<>();

        // Retrieve all the markers from the database.
        List<Integer> markers = instance.getGlobalSQL().getIntList("SELECT id FROM location_marker");

        markers.forEach(id -> {
            // Get the name
            String location = instance.getGlobalSQL().getString("SELECT location FROM location_marker WHERE id=" + id + ";");
            int coordinate_id = instance.getGlobalSQL().getInt("SELECT coordinate_id FROM location_marker WHERE id=" + id + ";");
            if (location == null) {
                // Load subcategory.
                int subcategory_id = instance.getGlobalSQL().getInt("SELECT subcategory FROM location_marker WHERE " + "id=" + id + ";");
                loadSubcategoryMarker(subcategory_id, coordinate_id);
            } else {
                loadLocationMarker(location, coordinate_id);
            }
        });
    }

    private void loadLocationMarker(String name, int coordinate_id) {

        Hologram hologram = createMarker(name, coordinate_id, false);

        if (hologram == null) {
            log.warning(String.format("Hologram %s was not created due to an error, a hologram with this name " + "probably already exists.", name));
            return;
        }

        // Create the click action.
        HologramClickAction clickAction = (NetworkUser u) -> teleportToLocation(u, name);
        holograms.put(hologram, clickAction);
    }

    private void loadSubcategoryMarker(int subcategory_id, int coordinate_id) {

        // Get subcategory name.
        String subcategory = instance.getGlobalSQL().getString("SELECT name FROM location_subcategory WHERE id=" + subcategory_id + ";");

        if (subcategory == null) {
            log.warning(String.format("Subcategory with id %d does not exist!", subcategory_id));
            return;
        }

        Hologram hologram = createMarker(subcategory, coordinate_id, true);

        if (hologram == null) {
            log.warning(String.format("Hologram %s was not created due to an error, a hologram with this name " + "probably already exists.", subcategory));
            return;
        }

        // Create the click action.
        HologramClickAction clickAction = (NetworkUser u) -> Bukkit.getScheduler().runTask(instance, () -> openSubcategoryMenu(u, subcategory));
        holograms.put(hologram, clickAction);
    }

    private Hologram createMarker(String name, int coordinate_id, boolean subcategory) {
        // Get location.
        Location l = instance.getGlobalSQL().getLocation(coordinate_id);

        // Create a hologram for the location.
        if (l.getWorld() == null) {
            log.warning(String.format("Unable to create hologram %s, world can not be found.", name));
            return null;
        }
        return Holograms.createHologram(name, l, appendColour(Arrays.asList(name, "↓"), subcategory));
    }

    private void teleportToLocation(NetworkUser u, String location) {
        // Get coordinate id.
        int coordinate_id = instance.getGlobalSQL().getInt("SELECT coordinate FROM location_data WHERE location='" + location + "';");

        // Get the server.
        String server = instance.getGlobalSQL().getString("SELECT server FROM coordinates WHERE id=" + coordinate_id + ";");

        if (server == null) {
            u.sendMessage(ChatUtils.error("An error occurred, please contact a server administrator."));
            return;
        }

        // Create teleport event.
        eventAPI.createTeleportEvent(true, u.player.getUniqueId().toString(), "teleport location " + location, LocationAdapter.adapt(u.player.getLocation()));

        serverAPI.switchServer(PlayerAdapter.adapt(u.player), server);
    }

    private void openSubcategoryMenu(NetworkUser u, String subcategory) {
        // Get the subcategory id.
        int id = instance.getGlobalSQL().getInt("SELECT id FROM location_subcategory WHERE name='" + subcategory + "';");

        if (id == 0) {
            u.sendMessage(ChatUtils.error("An error occurred, please contact a server administrator."));
            return;
        }

        // Get all locations for the subcategory.
        List<String> locations = instance.getGlobalSQL().getStringList("SELECT location FROM location_data WHERE " + "subcategory=" + id + ";");

        // Create temporary location menu.
        LocationMenu menu = new LocationMenu(guiProvider, subcategory, u, Category.TEMPORARY, null, locations.toArray(String[]::new));
        menu.setDeleteOnClose(true);

        // Open the menu.
        menu.open(u.player);
    }

    public interface HologramClickAction {
        void click(NetworkUser u);
    }
}