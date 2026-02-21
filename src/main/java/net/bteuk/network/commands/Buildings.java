package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.building_counter.Building;
import net.bteuk.network.building_counter.ConfirmationListener;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.commands.tabcompleters.TabCompleterTree;
import net.bteuk.network.commands.tabcompleters.TreeTabCompleter;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.enums.ServerType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bteuk.network.utils.Constants.SERVER_TYPE;

public class Buildings extends AbstractCommand {
    private static final Component ERROR = ChatUtils.error("/building add/show/count/delete/definition/query");

    private static final int SHOW_BUILDINGS_DURATION = 300;

    private final Network instance;
    private final List<Player> playersUsingConfirmationListeners = new ArrayList<Player>();
    ;

    public Buildings(Network instance) {
        super();
        this.instance = instance;
        setTabCompleter(new TreeTabCompleter(new TabCompleterTree("add, show, count (total, personal), delete, help, query, definition, help, flag (public, private, built, counted), claim, recent")));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }
        if (args.length < 1) {
            player.sendMessage(ERROR);
            return;
        }

        switch (args[0]) {
            case "add":
                if (SERVER_TYPE != ServerType.EARTH && SERVER_TYPE != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                if (player.hasPermission("network.buildings.add")) {

                    addBuilding(player);
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
            case "show":
                if (SERVER_TYPE != ServerType.EARTH && SERVER_TYPE != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                showBuildings(player);
                break;
            case "count":
                if (args.length > 1 && args[1].equals( "personal") )
                {
                    if (player.hasPermission("network.buildings.add")) {

                        displayPlayerCount(player);
                    } else {
                        player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                    }
                }
                displayCount(player);
                break;
            case "delete":
                if (SERVER_TYPE != ServerType.EARTH && SERVER_TYPE != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                deleteBuilding(player);
                break;
            case "definition":
                player.sendMessage(ChatUtils.success(
                        "A building is a structure that has walls on all sides, a roof, is larger than 2*3m and can be entered by a human (no sheds or caravans). In other words " +
                                "use common sense. A " +
                                "terrace is many buildings (one for each property). A semi detached is one building. Apartments are one building"));
                break;
            case "query":
                if (SERVER_TYPE != ServerType.EARTH && SERVER_TYPE != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                queryBuilding(player);
                break;
            case "help":
                player.sendMessage(ChatUtils.greyText("To add a building stand on top of the building and run /building add. You can see the buildings with /building show. If you want to delete a building you can run /building delete it will delete the closest building. To hide a building from the progress map you can give it a private flag with /flag or when adding with /building add -f private"));
                break;
            case "flag":
                if (SERVER_TYPE != ServerType.EARTH && SERVER_TYPE != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                if (player.hasPermission("network.buildings.add")) {

                    if (args.length > 1 )
                    {
                            updateBuildingFlags(player,args[1]);
                    }
                    else
                    {
                        player.sendMessage(ChatUtils.error("Please add a flag to change"));
                    }
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
            case "claim":
                if (SERVER_TYPE != ServerType.EARTH && SERVER_TYPE != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                if (player.hasPermission("network.buildings.add")) {

                    claimBuilding(player);
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
            case "recent":
                if (player.hasPermission("network.buildings.recent")) {

                }
                break;
        }

    }

    private void updateBuildingFlags(Player player, String flag)
    {
        Building minbuilding = getClosestBuilding(player);
        if (minbuilding == null){
            return;
        }
        if (minbuilding.playerId().equals(player.getUniqueId().toString()))
        {
            String field = "";
            String newValue = "";
            switch (flag)
            {
                case "private":
                    field = "isPublic";
                    newValue = "false";
                    break;
                case "public":
                    field ="isPublic";
                    newValue = "true";
                    break;
                case "built":
                    field = "playerBuilt";
                    newValue = "true";
                    break;
                case "counted":
                    field = "playerBuilt";
                    newValue = "false";
                    break;
                default:
                    player.sendMessage(ChatUtils.error("given flag doesn't exist"));
                    return;
            }
            instance.getGlobalSQL().update(String.format("UPDATE buildings SET %s = %s WHERE building_id = %d",field, newValue,minbuilding.buildingId() ));
        }
    }


    private void claimBuilding(Player player){
        Building minbuilding = getClosestBuilding(player);
        if (minbuilding == null){
            return;
        }
        if (!minbuilding.playerBuilt())
        {
            instance.getGlobalSQL().update(String.format( "UPDATE buildings SET player_id = '%s', playerBuilt = TRUE WHERE building_id = %d",player.getUniqueId(), minbuilding.buildingId() ));
            player.sendMessage(ChatUtils.success("Building builder updated to you"));
        }
        else
        {
            player.sendMessage(ChatUtils.error("This building is already claimed"));
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
            double currentDist = getDist(i.coordinate(), player.getLocation());
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

    public static double getDist(Location l1, Location l2) {
        double deltax = l1.getX() - l2.getX();
        double deltaz = l2.getZ() - l2.getZ();
        return Math.sqrt((deltax * deltax) + (deltaz * deltaz));
    }

    private void displayCount(Player player) {
        int buildingCount = instance.getGlobalSQL().getInt("SELECT COUNT(*) FROM buildings;");
        player.sendMessage(ChatUtils.success("%s buildings have been built!", String.valueOf(buildingCount)));
    }

    private void displayPlayerCount(Player player){
        int buildingCount = instance.getGlobalSQL().getInt(String.format("SELECT COUNT(*) FROM buildings WHERE player_id ='%s' AND playerBuilt = true",player.getUniqueId()));
        player.sendMessage(ChatUtils.success("you have built %s buildings!", String.valueOf(buildingCount)));

    }

    private void addMockBuilding(Player player)
    {
        Location l = player.getLocation();
        player.sendMessage(ChatUtils.success("Building added at %s,%s", String.valueOf(l.getX()), String.valueOf(l.getZ())));
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

}
