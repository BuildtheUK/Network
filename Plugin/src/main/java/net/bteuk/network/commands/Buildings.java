package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.building_counter.Building;
import net.bteuk.network.building_counter.ConfirmationListener;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.commands.tabcompleters.TabCompleterTree;
import net.bteuk.network.commands.tabcompleters.TreeTabCompleter;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.PlotSQL;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.maven.model.Build;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.bteuk.network.core.ServerType.PLOT;

public class Buildings extends AbstractCommand {
    private static final Component ERROR = ChatUtils.error("/building add/show/count/delete/definition/query");
    private final EarthGeneratorSettings bteGeneratorSettings =
            EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);

    private static final int SHOW_BUILDINGS_DURATION = 300;
    private final PlotSQL plotSQL;

    private final Network instance;
    private final Constants constants;
    private final List<Player> playersUsingConfirmationListeners = new ArrayList<>();

    public Buildings(Network instance, PlotSQL plotSQL, Constants constants) {
        super();
        this.instance = instance;
        this.plotSQL = plotSQL;
        setTabCompleter(new TreeTabCompleter(
                new TabCompleterTree("add, show, count (total, personal), delete, help, query, definition, help, flag (public, private, built, counted), claim, recent")));
        this.constants = constants;
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
                if (constants.serverType() != ServerType.EARTH && constants.serverType() != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                if (player.hasPermission("network.buildings.add")) {
                    String[] uniqueArgs;
                    if (args.length > 1 && args[1].equals("-f"))
                        uniqueArgs = Arrays.stream(args).skip(2).distinct().toArray(String[]::new);
                    else
                        uniqueArgs = new String[0];
                    addBuilding(player, uniqueArgs);
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
            case "show":
                if (constants.serverType() != ServerType.EARTH && constants.serverType() != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                showBuildings(player);
                break;
            case "count":
                if (args.length > 1 && args[1].equals("personal")) {
                    if (player.hasPermission("network.buildings.add")) {

                        displayPlayerCount(player);
                    } else {
                        player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                    }
                } else {
                    displayCount(player);
                }
                break;
            case "delete":
                if (constants.serverType() != ServerType.EARTH && constants.serverType() != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
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
                if (constants.serverType() != ServerType.EARTH && constants.serverType() != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                queryBuilding(player);
                break;
            case "help":
                player.sendMessage(ChatUtils.greyText(
                        "To add a building stand on top of the building and run /building add. You can see the buildings with /building show. If you want to delete a building " +
                                "you can run /building delete it will delete the closest building. To hide a building from the progress map you can give it a private flag with " +
                                "/flag or when adding with /building add -f private"));
                break;
            case "flag":
                if (constants.serverType() != ServerType.EARTH && constants.serverType() != ServerType.PLOT) {
                    player.sendMessage(ChatUtils.error("This command can't be run on this server"));
                    return;
                }
                if (player.hasPermission("network.buildings.add")) {

                    if (args.length > 1) {
                        updateBuildingFlags(player, args[1]);
                    } else {
                        player.sendMessage(ChatUtils.error("Please add a flag to change"));
                    }
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
            case "claim":
                if (constants.serverType() != ServerType.EARTH && constants.serverType() != ServerType.PLOT) {
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
                    if (args.length > 1) {
                        try {
                            int page = Integer.parseInt(args[1]); // note args[1], not args[0], since args[0] is command
                            displayMostRecent(player, page);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatUtils.error("Invalid page number. Using page 1."));
                            displayMostRecent(player, 1);
                        }
                    } else {
                        displayMostRecent(player, 1);
                    }
                } else {
                    player.sendMessage(ChatUtils.error("You don't have permission to use this command"));
                }
                break;
        }

    }

    private void displayMostRecent(Player player, int page) {

        int pageSize = 6;

        if (page < 1) {
            page = 1;
        }

        int offset = (page - 1) * pageSize;

        String condition = String.format(
                "WHERE is_public = true AND player_built = true ORDER BY time_added DESC LIMIT %d OFFSET %d",
                pageSize,
                offset
        );

        List<Building> recent = instance.getGlobalSQL().getBuildings(condition);

        if (recent.isEmpty()) {
            player.sendMessage(ChatUtils.error("No more buildings found."));
            return;
        }
        Component message = Component.text("");
        // Previous page button
        if (page > 1) {
            Component previousPage = Component.text("⏪⏪⏪", TextColor.color(212, 113, 15));
            previousPage = previousPage.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to view the previous page of recent buildings.")));
            previousPage = previousPage.clickEvent(ClickEvent.runCommand("/building recent " + (page - 1)));

            message = message.append(previousPage).append(Component.text(" "));
        }

        message = message.append(Component.text("Page ", NamedTextColor.GREEN)
                .append(Component.text(page, TextColor.color(245, 221, 100))));

        // Next page button (only show if full page returned)
        if (recent.size() == pageSize) {
            Component nextPage = Component.text(" ⏩⏩⏩\n", TextColor.color(212, 113, 15));
            nextPage = nextPage.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to view the next page of recent buildings.")));
            nextPage = nextPage.clickEvent(ClickEvent.runCommand("/building recent " + (page + 1)));
            message = message.append(Component.text(" "));
            message = message.append(nextPage);
        } else {
            message = message.append(Component.text("\n"));
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        int count = 0;
        for (Building b : recent) {
            String playerName = instance.getGlobalSQL().getString(String.format("SELECT name FROM player_data WHERE uuid='%s';", b.playerId()));
            String formattedDate = b.timeCreated().format(formatter);
            Component buildingComponent = Component.text("");
            buildingComponent = buildingComponent.append(
                    Component.text("# " + b.buildingId(), NamedTextColor.AQUA));
            buildingComponent = buildingComponent.append(
                    Component.text(" - builder: ", NamedTextColor.WHITE));
            buildingComponent = buildingComponent.append(
                    Component.text(playerName, NamedTextColor.GREEN));
            buildingComponent = buildingComponent.append(
                    Component.text(" | DateConstructed: ", NamedTextColor.WHITE));
            buildingComponent = buildingComponent.append(
                    Component.text(formattedDate, NamedTextColor.BLUE));
            buildingComponent = buildingComponent.hoverEvent(
                    HoverEvent.hoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Component.text("Click to teleport")
                    )
            );
            buildingComponent = buildingComponent.clickEvent(
                    ClickEvent.runCommand("/tpll " + b.lat() + ", " + b.lon())
            );
            buildingComponent = buildingComponent.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to teleport")));
            buildingComponent = buildingComponent.clickEvent(ClickEvent.runCommand("/tpll " + b.lat() + ", " + b.lon()));
            message = message.append(buildingComponent);
            count++;
            if (count < recent.size()) {
                message = message.append(Component.text("\n", NamedTextColor.WHITE));
            }
        }

        player.sendMessage(message);
    }

    private void updateBuildingFlags(Player player, String flag) {
        Building minbuilding = getClosestBuilding(player);
        if (minbuilding == null) {
            return;
        }
        if (minbuilding.playerId().equals(player.getUniqueId().toString()) || player.hasPermission("network.buildings.update")) {
            boolean isPublic = instance.getGlobalSQL().getBoolean(String.format("SELECT is_public FROM buildings WHERE building_id = %d", minbuilding.buildingId()));
            boolean isBuilt = instance.getGlobalSQL().getBoolean(String.format("SELECT player_built FROM buildings WHERE building_id = %d", minbuilding.buildingId()));
            switch (flag) {
                case "private":
                    if (!isBuilt) {
                        player.sendMessage(ChatUtils.error("This flag can't be given to a building that you have marked as counted only"));
                        break;
                    }
                    instance.getGlobalSQL().update(String.format("UPDATE buildings SET is_public = false WHERE building_id = %d", minbuilding.buildingId()));
                    player.sendMessage(ChatUtils.success("Building is now private"));
                    break;
                case "public":
                    instance.getGlobalSQL().update(String.format("UPDATE buildings SET is_public = true WHERE building_id = %d", minbuilding.buildingId()));
                    player.sendMessage(ChatUtils.success("Building is now public"));
                    break;
                case "built":
                    instance.getGlobalSQL().update(String.format("UPDATE buildings SET player_built = true WHERE building_id = %d", minbuilding.buildingId()));
                    player.sendMessage(ChatUtils.success("Building will now count towards your personal total"));
                    break;
                case "counted":
                    if (!isPublic) {
                        player.sendMessage(ChatUtils.error("This flag can't be given to a building that you have marked as private"));
                        break;
                    }
                    instance.getGlobalSQL().update(String.format("UPDATE buildings SET player_built = false WHERE building_id = %d", minbuilding.buildingId()));
                    player.sendMessage(ChatUtils.success("Building will no longer count towards your personal total. This building can now be claimed by someone else"));
                    break;
                default:
                    player.sendMessage(ChatUtils.error("given flag doesn't exist"));
                    return;
            }
        } else {
            player.sendMessage(ChatUtils.error("You don't have permission to update this building"));
        }
    }

    private void claimBuilding(Player player) {
        Building minbuilding = getClosestBuilding(player);
        if (minbuilding == null) {
            return;
        }
        if (!minbuilding.playerBuilt()) {
            instance.getGlobalSQL()
                    .update(String.format("UPDATE buildings SET player_id = '%s', player_built = TRUE WHERE building_id = %d", player.getUniqueId(), minbuilding.buildingId()));
            player.sendMessage(ChatUtils.success("Building builder updated to you"));
        } else {
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
        double minDist = Double.MAX_VALUE;
        Building minbuilding = null;
        for (Building i : nearbyBuildings) {
            double currentDist = getXZDistance(i.coordinate(), player.getLocation());
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

    public static double getXZDistance(Location l1, Location l2) {
        double deltax = l1.getX() - l2.getX();
        double deltaz = l1.getZ() - l2.getZ();
        return Math.sqrt((deltax * deltax) + (deltaz * deltaz));
    }

    private void displayCount(Player player) {
        int buildingCount = instance.getGlobalSQL().getInt("SELECT COUNT(*) FROM buildings;");
        player.sendMessage(ChatUtils.success("%s buildings have been built!", String.valueOf(buildingCount)));
    }

    private void displayPlayerCount(Player player) {
        int buildingCount = instance.getGlobalSQL().getInt(String.format("SELECT COUNT(*) FROM buildings WHERE player_id ='%s' AND player_built = true", player.getUniqueId()));
        if (buildingCount != 1) {
            player.sendMessage(ChatUtils.success("You have built %s buildings!", String.valueOf(buildingCount)));
        } else {
            player.sendMessage(ChatUtils.success("You have built 1 building!"));
        }

    }

    private void addMockBuilding(Player player) {
        Location l = player.getLocation();
        player.sendMessage(ChatUtils.success("Building added at %s,%s", String.valueOf(l.getX()), String.valueOf(l.getZ())));
    }

    public void addPlayerToListenerList(Player player) {
        playersUsingConfirmationListeners.add(player);
    }

    public void removePlayerFromListenerList(Player player) {
        playersUsingConfirmationListeners.remove(player);
    }

    private void addBuilding(Player player, String[] flags) {
        List<Building> nearbyBuildings = getNearbyBuildings(player, 20);
        if (playersUsingConfirmationListeners.contains(player)) {
            player.sendMessage(ChatUtils.error("Please respond to your current building add attempt before adding a new building."));
            return;
        }
        if (!nearbyBuildings.isEmpty()) {
            player.sendMessage(ChatUtils.error("Other buildings nearby, to confirm a new building being added type 'y'. If unsure type 'n' and run /building show."));
            new ConfirmationListener(this, player.getLocation(), player, instance, flags);
        } else {
            addBuildingToDataBase(player, player.getLocation(), flags);
        }
    }

    private double[] getPlayerIRLCoords(Player player) {
        try {
            int deltaX = 0;
            int deltaZ = 0;
            if (constants.serverType() == PLOT && plotSQL.hasRow("SELECT name FROM location_data WHERE name='" + player.getWorld().getName() + "';")) {
                // Get negative coordinate transform of new location.
                deltaX = -plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + player.getWorld().getName() + "';");
                deltaZ = -plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + player.getWorld().getName() + "';");
            }
            double[] coords = bteGeneratorSettings.projection().toGeo(player.getLocation().getX() + deltaX,
                    player.getLocation().getZ() + deltaZ);
            return coords;
        } catch (
                OutOfProjectionBoundsException e) {
            throw new RuntimeException("You are not standing in a location where coordinates can be retrieved.");
        }
    }

    public void addBuildingToDataBase(Player player, Location l, String[] flags) {

        int coordinateId = instance.getGlobalSQL().addCoordinate(l);

        try {
            double[] coords = getPlayerIRLCoords(player);

            boolean isPublic = true;
            boolean playerBuilt = true;
            boolean isConflict = false;
            for (String f : flags) {
                switch (f) {
                    case "private":
                        if (playerBuilt)
                            isPublic = false;
                        else
                            isConflict = true;
                        break;
                    case "public":
                        isPublic = true;
                        break;
                    case "built":
                        playerBuilt = true;
                        break;
                    case "counted":
                        if (isPublic)
                            playerBuilt = false;
                        else
                            isConflict = true;
                        break;
                    default:
                        break;
                }
            }
            if (isConflict) {
                player.sendMessage(ChatUtils.error("Incompatible flags given. Resorting to default flags. These can be changed with /flag later"));
                isPublic = true;
                playerBuilt = true;
            }
            instance.getGlobalSQL()
                    .update(String.format("INSERT INTO buildings (coordinate_id, player_id, is_public, player_built, lat, lon) VALUES (%d, '%s' ,%b, %b, %f ,%f);", coordinateId,
                            player.getUniqueId(), isPublic, playerBuilt, coords[1], coords[0]));
            player.sendMessage(ChatUtils.success("Building added at %s,%s", String.valueOf(coords[0]), String.valueOf(coords[1])));
        } catch (RuntimeException e) {
            player.sendMessage(ChatUtils.error(e.getMessage()));
        }
    }

    private void queryBuilding(Player player) {
        Building b = getClosestBuilding(player);
        if (b == null) {
            return;
        }
        String playerName = instance.getGlobalSQL().getString(String.format("SELECT name FROM player_data WHERE uuid='%s';", b.playerId()));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String formattedDate = b.timeCreated().format(formatter);

        if (b.isPublic() && b.playerBuilt()) {
            player.sendMessage(ChatUtils.success(String.format("Building ID: %d, Player: %s, Date Added: %s", b.buildingId(), playerName, formattedDate)));
        } else if (b.isPublic() && !b.playerBuilt()) {
            player.sendMessage(ChatUtils.success(String.format("Building ID: %d, Unclaimed, Date Added: %s", b.buildingId(), formattedDate)));
        } else if (b.playerId().equals(player.getUniqueId().toString())) {
            player.sendMessage(ChatUtils.success(String.format("Building ID: %d, Player: %s, Date Added: %s", b.buildingId(), playerName, formattedDate)));
        } else {
            player.sendMessage(ChatUtils.success(String.format("Building ID: %d", b.buildingId())));
        }

    }

    private Location geoToWorld(double lat, double lon, World world, int deltaX, int deltaZ) throws OutOfProjectionBoundsException {

        double[] xz = bteGeneratorSettings.projection().fromGeo(lon, lat);

        double x = xz[0];
        double z = xz[1];

        x -= deltaX;
        z -= deltaZ;

        return new Location(world, x, 0, z);
    }

    private void showBuildings(Player player) {
        List<Building> nearbyBuildings = getNearbyBuildings(player, 100);
        // StringBuilder locs = new StringBuilder("buildings nearby:");
        List<Location> heightBuildingsAdded = new ArrayList<Location>();
        int deltaX = 0;
        int deltaZ = 0;
        if (constants.serverType() == PLOT && plotSQL.hasRow("SELECT name FROM location_data WHERE name='" + player.getWorld().getName() + "';")) {
            deltaX = -plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + player.getWorld().getName() + "';");
            deltaZ = -plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + player.getWorld().getName() + "';");
        }
        for (Building j : nearbyBuildings) {
            Location i;
            if (j.coordinate().getWorld() != null && j.coordinate().getWorld().equals(player.getWorld())) {

                i = j.coordinate();
            } else {

                try {
                    i = geoToWorld(j.lat(), j.lon(), player.getWorld(), deltaX, deltaZ);
                } catch (Exception e) {
                    continue;
                }

            }
            // locs.append(" (").append(Math.round(i.getX())).append(",").append(Math.round(i.getZ())).append("),");
            Location finalHeight = new Location(player.getWorld(), i.getX(), player.getWorld().getHighestBlockYAt(i) - 1, i.getZ());
            heightBuildingsAdded.add(finalHeight);
            player.sendBlockChange(finalHeight, Material.BEACON.createBlockData());
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Location ironLoc = finalHeight.clone().add(x, -1, z);
                    player.sendBlockChange(ironLoc, Material.IRON_BLOCK.createBlockData());
                }
            }

            Location glassLoc = finalHeight.clone().add(0, 1, 0);
            if (!j.playerBuilt()) {
                player.sendBlockChange(glassLoc, Material.ORANGE_STAINED_GLASS.createBlockData());
            } else if (j.playerId().equals(player.getUniqueId().toString())) {
                player.sendBlockChange(glassLoc, Material.GREEN_STAINED_GLASS.createBlockData());
            } else {
                player.sendBlockChange(glassLoc, Material.RED_STAINED_GLASS.createBlockData());
            }
        }
        instance.getServer().getScheduler().runTaskLater(instance, () -> removeDisplayBeacons(player, heightBuildingsAdded), SHOW_BUILDINGS_DURATION);
        player.sendMessage(ChatUtils.success(nearbyBuildings.size() + " nearby buildings have been shown"));
    }

    private List<Building> getNearbyBuildings(Player player, int radius) {
        double[] pl = new double[2];
        try {
            pl = getPlayerIRLCoords(player);
        } catch (RuntimeException e) {
            player.sendMessage(ChatUtils.error(e.getMessage()));
            return new ArrayList<Building>();
        }
        double M_PER_DEGREE = 111320.0;
        double latDelta = radius / M_PER_DEGREE;
        double lonDelta = radius / (M_PER_DEGREE * Math.cos(Math.toRadians(pl[1])));

        double lonmax = pl[0] + lonDelta;
        double lonmin = pl[0] - lonDelta;
        double latmax = pl[1] + latDelta;
        double latmin = pl[1] - latDelta;
        String condition = String.format("WHERE buildings.lat > %f AND buildings.lat < %f AND buildings.lon > %f AND buildings.lon < %f", latmin, latmax, lonmin, lonmax);
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
// TO DO - make recent get data from a join so one request instead of many
