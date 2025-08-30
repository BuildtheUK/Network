package net.bteuk.network.lobby;

/*
This class will handle the functionality of the lobby that are exclusive to the lobby server.

Features of the lobby include but are not limited to:
    Interactive Leaderboards
    Interactive Map
    Portals

The reason the lobby functions have been separated is to prevent unnecessary resource usage on the non-lobby server.
 */

import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import net.bteuk.network.CommandManager;
import net.bteuk.network.Network;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.core.Constants;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

@Log
public class Lobby {

    private final Network instance;
    private final Constants constants;
    private final ServerAPI serverAPI;
    private final EventManager eventManager;

    @Setter
    private GuiProvider guiProvider;

    private final ArrayList<Portal> portals;
    @Getter
    private Location spawn;
    private int portalTask;
    private Book rulesBook;
    private Map map;

    public Lobby(Network instance, Constants constants, ServerAPI serverAPI, EventManager eventManager) {

        this.instance = instance;
        this.constants = constants;
        this.serverAPI = serverAPI;
        this.eventManager = eventManager;
        portals = new ArrayList<>();
    }

    // A public method to reload portals from the portals.yml file.
    // This method is run when using the specific reload command.
    // It saves having to restart the server when portals are edited.
    public void reloadPortals() {

        // Clear the portals arrayList and stop the portals from running.
        if (portalTask != 0) {
            instance.getTimerAPI().cancelTimer(portalTask);
        }
        portals.clear();

        // Load the portals from config.
        loadPortals();
    }

    // Reads the portals from portals.yml
    private void loadPortals() {

        // Create portals.yml if not exists.
        // The data folder should already exist since the plugin will always create config.yml first.
        File portalsFile = new File(instance.getDataFolder(), "portals.yml");
        if (!portalsFile.exists()) {
            instance.saveResource("portals.yml", false);
        }

        FileConfiguration portalsConfig = YamlConfiguration.loadConfiguration(portalsFile);

        // Gets all the portal names from the config.
        // This will allow us to query the config for portals.
        ConfigurationSection section = portalsConfig.getConfigurationSection("portals");
        // No portals have yet been added.
        if (section == null) {
            return;
        }

        Set<String> portalNames = Objects.requireNonNull(portalsConfig.getConfigurationSection("portals")).getKeys(false);

        // Create the portal from the config.
        for (String portalName : portalNames) {

            // Create new portal with given values from config.
            try {

                portals.add(new Portal(eventManager, portalsConfig.getInt("portals." + portalName + ".min.x"), portalsConfig.getInt("portals." + portalName + ".min.y"),
                        portalsConfig.getInt("portals." + portalName + ".min.z"), portalsConfig.getInt("portals." + portalName + ".max.x"),
                        portalsConfig.getInt("portals." + portalName + ".max.y"), portalsConfig.getInt("portals." + portalName + ".max.z"),
                        Objects.requireNonNull(portalsConfig.getString("portals." + portalName + ".executes")).split(",")));
            } catch (Exception e) {
                log.warning("Portal " + portalName + " configured incorrectly, please " + "check the portals.yml file.");
            }
        }

        // Once the portals have been loaded, start running them.
        runPortals();
    }

    // Runs the events set to the portals.
    // Events will run each second in order of how they are in the portals.yml file.
    private void runPortals() {

        portalTask = instance.getTimerAPI().registerTimer(() -> {

            // Check if any players are in the area of the portal.
            for (NetworkUser user : instance.getUsers()) {

                user.inPortal = false;

                for (Portal portal : portals) {

                    if (portal.in(user.player.getLocation())) {
                        user.inPortal = true;
                        if (!user.wasInPortal) {

                            // Set user to in portal.
                            user.wasInPortal = true;

                            // If they are run the portal events.
                            portal.event(user.player);
                        }
                        break;
                    }
                }

                if (user.wasInPortal && !user.inPortal) {

                    // Set user to not in portal.
                    user.wasInPortal = false;
                }
            }
        }, 1L);
    }

    // Load the rules.
    // The rules are stored in rules.yml.
    public void loadRules() {

        // Create rules.yml if not exists.
        // The data folder should already exist since the plugin will always create config.yml first.
        File rulesFile = new File(instance.getDataFolder(), "rules.yml");
        if (!rulesFile.exists()) {
            instance.saveResource("rules.yml", false);
        }

        FileConfiguration rulesConfig = YamlConfiguration.loadConfiguration(rulesFile);

        // Gets all the portal names from the config.
        // This will allow us to query the config for portals.
        ConfigurationSection section = rulesConfig.getConfigurationSection("rules");
        // No portals have yet been added.
        if (section == null) {
            return;
        }

        Set<String> rules = Objects.requireNonNull(rulesConfig.getConfigurationSection("rules")).getKeys(false);

        // Create book.
        Component title = Component.text("Rules", NamedTextColor.AQUA, TextDecoration.BOLD);
        Component author = Component.text("Unknown");

        String sAuthor = rulesConfig.getString("author");
        if (sAuthor != null) {
            author = Component.text(sAuthor);
        }

        // Set pages of the book.
        ArrayList<Component> pages = new ArrayList<>();
        rules.forEach(str -> pages.add(Component.text(Objects.requireNonNull(rulesConfig.getString("rules." + str)))));

        rulesBook = Book.book(title, author, pages);
    }

    public Book getRules() {
        return rulesBook;
    }

    // Set the lectern with the rules in the world.
    public void setLectern() {

        // Load rules.yml, it is guaranteed to exist since that is check in loadRules(), which has to be run before
        // this.
        File rulesFile = new File(instance.getDataFolder(), "rules.yml");
        FileConfiguration rulesConfig = YamlConfiguration.loadConfiguration(rulesFile);

        String worldName = rulesConfig.getString("location.world");

        if (worldName == null) {
            instance.getLogger().warning("No world set in rules.yml, rules lectern can not be set.");
            return;
        }

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            instance.getLogger().warning("Lobby world is null, rules lectern can not be set.");
            return;
        }

        Location l = new Location(world, rulesConfig.getInt("location.x"), rulesConfig.getInt("location.y"), rulesConfig.getInt("location.z"));

        // Check if plot is lectern.
        if (!(world.getType(l) == Material.LECTERN)) {
            instance.getLogger().warning("There is no lectern at the specified coordinates in rules.yml.");
            return;
        }

        // Add listener
        new TakeBookEvent(instance);
    }

    private void runLeaderboards() {

    }

    /**
     * Reloads the map, this will be run when the plugin is enabled.
     * As well as when the map is manually reloaded using /lobby reload map.
     */
    public void reloadMap(CommandManager commandManager) {

        if (map == null) {
            map = new Map(instance, constants, serverAPI, eventManager, guiProvider);
        }
        map.reload();
        map.registerMapCommand(commandManager);
    }

    public void setSpawn() {

        try {
            spawn = new Location(Bukkit.getWorld(Objects.requireNonNull(constants.spawnLocation().world())), constants.spawnLocation().x(), constants.spawnLocation().y(),
                    constants.spawnLocation().z(), constants.spawnLocation().yaw(), constants.spawnLocation().pitch());
        } catch (Exception e) {
            instance.getLogger().warning("Spawn location could not be set!");
            // Set default spawn.
            spawn = new Location(Bukkit.getWorlds().getFirst(), 0.0, 65.0, 0.0, 0, 0);
        }
    }

    public void enableVoidTeleport() {
        new VoidTeleport(instance, this);
    }
}
