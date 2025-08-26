package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.building_counter.Building;
import net.bteuk.network.building_counter.ConfirmationListener;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.lib.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Buildings extends AbstractCommand {
    private static final Component ERROR = ChatUtils.error("/building add/show/count/delete/definition/query");

    private static final int SHOW_BUILDINGS_DURATION = 300;

    private final Network instance;
    private final Constants constants;
    private final List<Player> playersUsingConfirmationListeners = new ArrayList<>();
    ;

    public Buildings(Network instance, Constants constants) {
        super();
        this.instance = instance;
        this.constants = constants;
        setTabCompleter(new FixedArgSelector(Arrays.asList("add", "show", "count", "delete", "definition", "query"), 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }
        System.out.println(args.length);
        if (args.length < 1) {
            player.sendMessage(ERROR);
            return;
        }

        switch (args[0]) {
            case "add":
                if (constants.serverType() != ServerType.EARTH) {
                    player.sendMessage(ChatUtils.error("Please join the earth server before running this command"));
                    return;
                }
                if (player.hasPermission("network.buildings.add")) {

                    addBuilding(player);
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
            case "show":
                if (constants.serverType() != ServerType.EARTH) {
                    player.sendMessage(ChatUtils.error("Please join the earth server before running this command"));
                    return;
                }
                showBuildings(player);
                break;
            case "count":
                displayCount(player);
                break;
            case "delete":
                if (constants.serverType() != ServerType.EARTH) {
                    player.sendMessage(ChatUtils.error("Please join the earth server before running this command"));
                    return;
                }
                deleteBuilding(player);
                break;
            case "definition":
                player.sendMessage(ChatUtils.success(
                        "A building is a structure that has walls on all sides, a roof, is larger than 2*3m and can be entered by a human (no sheds or caravans). In other words "
                                + "use common sense. A " + "terrace is many buildings (one for each property). A semi detached is one building. Apartments are one building"));
                break;
            case "query":
                if (constants.serverType() != ServerType.EARTH) {
                    player.sendMessage(ChatUtils.error("Please join the earth server before running this command"));
                    return;
                }
                queryBuilding(player);
                break;
        }
    }

    private void deleteBuilding(Player player) {
        Building minbuilding = getClosestBuilding(player);
        if (minbuilding == null) {
            return;
        }
        if (minbuilding.playerId().equals(player.getUniqueId().toString()) || player.hasPermission("network.buildings.delete")) {
            instance.getGlobalSQL().deleteBuilding(minbuilding);
            player.sendMessage(ChatUtils.success("Building deleted"));
        } else {
            player.sendMessage(ChatUtils.error("You don't have permission to delete this building"));

        }

    }

    private Building getClosestBuilding(Player player) {
        List<Building> nearbyBuildings = getNearbyBuildings(player, 5);
        double minDist = 100;
        Building minbuilding = null;
        for (Building i : nearbyBuildings) {
            double currentDist = i.coordinate().distance(player.getLocation());
            if (currentDist < minDist) {
                minDist = currentDist;
                minbuilding = i;
            }
        }
        if (minbuilding == null) {
            player.sendMessage(ChatUtils.error("No buildings within 5 blocks"));
        }
        return minbuilding;
    }

    private void displayCount(Player player) {
        int buildingCount = instance.getGlobalSQL().getInt("SELECT COUNT(*) FROM buildings;");
        player.sendMessage(ChatUtils.success("%s buildings have been built!", String.valueOf(buildingCount)));
    }

    public void addPlayerToListenerList(Player player) {
        playersUsingConfirmationListeners.add(player);
    }

    public void removePlayerFromListenerList(Player player) {
        playersUsingConfirmationListeners.remove(player);
    }

    private void addBuilding(Player player) {
        List<Building> nearbyBuildings = getNearbyBuildings(player, 20);
        if (playersUsingConfirmationListeners.contains(player)) {
            player.sendMessage(ChatUtils.error("Please respond to your current building add attempt before adding a new building."));
            return;
        }
        // StringBuilder locs = new StringBuilder("buildings nearby:");
        // for (Building j : nearbyBuildings) {
        //     Location i = j.coordinate();
        //     locs.append(" (").append(Math.round(i.getX())).append(",").append(Math.round(i.getZ())).append("),");
        // }
        if (!nearbyBuildings.isEmpty()) {
            // locs.deleteCharAt(locs.length() - 1);
            player.sendMessage(ChatUtils.error("Other buildings nearby, to confirm a new building being added type 'y'. If unsure type 'n' and run /building show."));
            new ConfirmationListener(this, player.getLocation(), player, instance);
        } else {
            addBuildingToDataBase(player, player.getLocation());
        }
    }

    public void addBuildingToDataBase(Player player, Location l) {
        player.sendMessage(ChatUtils.success("Building added at %s,%s", String.valueOf(l.getX()), String.valueOf(l.getZ())));
        int coordinateId = instance.getGlobalSQL().addCoordinate(l);
        instance.getGlobalSQL().update(String.format("INSERT INTO buildings (coordinate_id, player_id) VALUES (%d, '%s');", coordinateId, player.getUniqueId()));
    }

    private void queryBuilding(Player player) {
        Building b = getClosestBuilding(player);
        if (b == null) {
            return;
        }
        String playerName = instance.getGlobalSQL().getString(String.format("SELECT name FROM player_data WHERE uuid='%s';", b.playerId()));
        player.sendMessage(ChatUtils.success(String.format("Building ID: %d, Player: %s", b.buildingId(), playerName)));

    }

    private void showBuildings(Player player) {
        List<Building> nearbyBuildings = getNearbyBuildings(player, 100);
        // StringBuilder locs = new StringBuilder("buildings nearby:");
        List<Location> heightBuildingsAdded = new ArrayList<Location>();
        for (Building j : nearbyBuildings) {
            Location i = j.coordinate();
            // locs.append(" (").append(Math.round(i.getX())).append(",").append(Math.round(i.getZ())).append("),");
            Location finalHeight = new Location(i.getWorld(), i.getX(), i.getWorld().getHighestBlockYAt(i) - 1, i.getZ());
            heightBuildingsAdded.add(finalHeight);
            player.sendBlockChange(finalHeight, Material.BEACON.createBlockData());
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location ironLoc = finalHeight.clone().add(x, -1, z);
                    player.sendBlockChange(ironLoc, Material.IRON_BLOCK.createBlockData());
                }
            }

            Location glassLoc = finalHeight.clone().add(0, 1, 0);
            if (j.playerId().equals(player.getUniqueId().toString())) {
                player.sendBlockChange(glassLoc, Material.GREEN_STAINED_GLASS.createBlockData());
            } else {
                player.sendBlockChange(glassLoc, Material.RED_STAINED_GLASS.createBlockData());
            }
            instance.getServer().getScheduler().runTaskLater(instance, () -> removeDisplayBeacons(player, heightBuildingsAdded), SHOW_BUILDINGS_DURATION);
        }
        // if (!nearbyBuildings.isEmpty()) {
        //     locs.deleteCharAt(locs.length() - 1);
        //     player.sendMessage(ChatUtils.success(locs.toString()));
        // }
    }

    private List<Building> getNearbyBuildings(Player player, int radius) {
        Location Pl = player.getLocation();
        double xmax = Pl.getX() + radius;
        double xmin = Pl.getX() - radius;
        double zmax = Pl.getZ() + radius;
        double zmin = Pl.getZ() - radius;
        String condition = String.format("WHERE coordinates.x > %f AND coordinates.x < %f AND coordinates.z > %f AND coordinates.z < %f", xmin, xmax, zmin, zmax);
        return instance.getGlobalSQL().getBuildings(condition);
    }

    private void removeDisplayBeacons(Player player, List<Location> nearbyBuildings) {
        for (Location i : nearbyBuildings) {
            Location glassLoc = i.clone().add(0, 1, 0);
            player.sendBlockChange(i, i.getBlock().getBlockData());
            player.sendBlockChange(glassLoc, glassLoc.getBlock().getBlockData());
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location ironLoc = i.clone().add(x, -1, z);
                    player.sendBlockChange(ironLoc, ironLoc.getBlock().getBlockData());
                }
            }
        }

    }

    @Override
    public String getLabel() {
        return "building";
    }

    @Override
    public String getDescription() {
        return "adds or shows completed buildings";
    }
}
