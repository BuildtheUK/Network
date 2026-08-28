package net.bteuk.network.commands.navigation;

import io.papermc.lib.PaperLib;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.papercore.WorldUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.Statistics;
import net.bteuk.network.utils.TpllFormat;
import net.bteuk.network.utils.Utils;
import net.buildtheearth.terraminusminus.dataset.IScalarDataset;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorPipelines;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.generator.GeneratorDatasets;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.buildtheearth.terraminusminus.util.geo.CoordinateParseUtils;
import net.buildtheearth.terraminusminus.util.geo.LatLng;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Tpll extends AbstractCommand {

    public static final EarthGeneratorSettings bteGeneratorSettings = EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("##.#####");
    private static final Component USAGE = ChatUtils.error("/tpll <latitude> <longitude> [altitude]");
    private final Network instance;
    private final boolean requiresPermission;
    private final RegionManager regionManager;
    private final Constants constants;
    private final PlotSQL plotSQL;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final GlobalSQL globalSQL;
    private final PreviousLocationTracker previousLocationTracker;

    public Tpll(Network instance, boolean requiresPermission, RegionManager regionManager, Constants constants, PlotSQL plotSQL, EventAPI eventAPI, ServerAPI serverAPI,
                GlobalSQL globalSQL, PreviousLocationTracker previousLocationTracker) {
        this.instance = instance;
        this.requiresPermission = requiresPermission;
        this.regionManager = regionManager;
        this.constants = constants;
        this.plotSQL = plotSQL;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;
        this.globalSQL = globalSQL;
        this.previousLocationTracker = previousLocationTracker;
    }

    /**
     * Convert the input arguments to a usable format
     *
     * @param args the command arguments
     * @return {@link TpllFormat} that includes the coordinate information that could be read from the command
     */
    public static TpllFormat getUsableTpllFormat(String[] args) {
        TpllFormat format = new TpllFormat();

        format.setCoordinates(CoordinateParseUtils.parseVerbatimCoordinates(getRawArguments(args).trim()));

        if (format.getCoordinates() == null) {
            LatLng possiblePlayerCoords = CoordinateParseUtils.parseVerbatimCoordinates(getRawArguments(selectArray(args)));
            if (possiblePlayerCoords != null) {
                format.setCoordinates(possiblePlayerCoords);
            }
        }

        LatLng possibleHeightCoords = CoordinateParseUtils.parseVerbatimCoordinates(getRawArguments(inverseSelectArray(args, args.length - 1)));
        if (possibleHeightCoords != null) {
            format.setCoordinates(possibleHeightCoords);
            try {
                format.setAltitude(Double.parseDouble(args[args.length - 1]));
            } catch (Exception ignored) {
            }
        }

        LatLng possibleHeightNameCoords = CoordinateParseUtils.parseVerbatimCoordinates(getRawArguments(inverseSelectArray(selectArray(args), selectArray(args).length - 1)));
        if (possibleHeightNameCoords != null) {
            format.setCoordinates(possibleHeightNameCoords);
            try {
                format.setAltitude(Double.parseDouble(selectArray(args)[selectArray(args).length - 1]));
            } catch (Exception ignored) {
            }
        }

        return format;
    }

    /**
     * Apply a coordinate transformation if the region is in the plot system.
     *
     * @param region the region to check
     * @param l      the location of the tpll
     * @return {@link Location} the location with potential coordinate transform
     */
    public Location applyCoordinateTransformIfPlotSystem(Region region, Location l) {

        // Regions must be enabled to use the plot system.
        if (constants.regionsEnabled()) {

            // Check if the region is on a plot server.
            if (regionManager.isPlot(region)) {
                String location = plotSQL.getString("SELECT location FROM regions WHERE " + "region='" + region.regionName() + "';");

                // Get the coordinate transformations.
                int xTransform = plotSQL.getInt("SELECT xTransform FROM location_data " + "WHERE name='" + location + "';");
                int zTransform = plotSQL.getInt("SELECT zTransform FROM location_data " + "WHERE name='" + location + "';");

                Location newLocation = l.clone();
                newLocation.setX(l.getX() + xTransform);
                newLocation.setZ((l.getZ() + zTransform));
                return newLocation;
            }
        }
        return l;
    }

    /**
     * Gets all objects in a string array above a given index
     *
     * @param args Initial array
     * @return Selected array
     */
    private static String[] selectArray(String[] args) {
        List<String> array = new ArrayList<>();

        if (args.length > 1) {
            array.addAll(Arrays.asList(args).subList(1, args.length));
        }

        return array.toArray(array.toArray(new String[0]));
    }

    private static String[] inverseSelectArray(String[] args, int index) {
        List<String> array = new ArrayList<>();

        if (index > 0) {
            array.addAll(Arrays.asList(args).subList(0, index));
        }

        return array.toArray(array.toArray(new String[0]));
    }

    private static String getRawArguments(String[] args) {
        if (args.length == 0) {
            return "";
        }
        if (args.length == 1) {
            return args[0];
        }

        StringBuilder arguments = new StringBuilder(args[0].replace((char) 176, (char) 32).trim());

        for (int x = 1; x < args.length; x++) {
            arguments.append(" ").append(args[x].replace((char) 176, (char) 32).trim());
        }

        return arguments.toString();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        // Only players can use /tpll.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check if permission is required.
        if (requiresPermission) {
            if (!player.hasPermission("uknet.navigation.tpll")) {
                player.sendMessage(NO_PERMISSION);
                return;
            }
        }

        tpll(player, args, false);
    }

    public void tpll(Player p, String[] args, boolean fromEvent) {

        // Check if there is at least 1 argument.
        if (args.length == 0) {
            p.sendMessage(USAGE);
            return;
        }

        // Convert the input to a usable format.
        TpllFormat format = getUsableTpllFormat(args);

        if (format.getCoordinates() == null) {
            p.sendMessage(USAGE);
            return;
        }

        double[] proj;

        try {
            proj = bteGeneratorSettings.projection().fromGeo(format.getCoordinates().getLng(), format.getCoordinates().getLat());
        } catch (Exception e) {
            p.sendMessage(USAGE);
            return;
        }
        // Get location and region.
        Location l = new Location(p.getWorld(), proj[0], 1, proj[1], p.getLocation().getYaw(), p.getLocation().getPitch());

        Region region = null;
        if (constants.regionsEnabled()) {
            region = regionManager.getRegion(proj[0], proj[1]);

            // Check if the player is allowed to teleport here.
            if (!canTeleportHere(p, region)) {
                p.sendMessage(ChatUtils.error("The terrain for this region has not been generated, " +
                        "you do not have permission to load new terrain."));
                return;
            }

            if (!constants.standalone()) {
                // Check the server of the location.
                // Switch if necessary.
                if (switchServerIfNecessary(p, region, args)) {
                    p.sendMessage(ChatUtils.success("The location is on another server, switching servers..."));
                    return;
                }
            }
        }

        // If the region is in the plot system, apply the coordinate transform.
        if (constants.plotSystemEnabled()) {
            l = applyCoordinateTransformIfPlotSystem(region, l);
        }

        // Set the correct world.
        if (constants.regionsEnabled()) {
            setWorldOfRegion(region, l);
        }

        // Check if the chunk has already been generated.
        // If not warn the player that it needs to be generated.
        CompletableFuture<Double> altFuture = getAltitude(p, format, l);
        if (altFuture == null) {
            return;
        }

        teleport(p, altFuture, format, l, fromEvent);
    }

    /**
     * Check if the player is allowed to teleport here.
     *
     * @param p      the player to check
     * @param region the region to check
     * @return whether the player can teleport here
     */
    private boolean canTeleportHere(Player p, Region region) {
        return regionManager.inDatabase(region) || p.hasPermission("uknet.regions.generate");
    }

    /**
     * Check of the region is on the current server, else switch server.
     *
     * @param region the region to check
     * @return whether the player is switching server
     */
    private boolean switchServerIfNecessary(Player p, Region region, String[] args) {
        // Check if the server of the region equals the current server, else teleport them with a teleport event
        // for tpll.
        String server = regionManager.getServer(region);
        if (!server.equals(constants.serverName())) {

            // Create teleport event.
            eventAPI.createTeleportEvent(true, p.getUniqueId().toString(), "teleport tpll " + String.join(" ", args), LocationAdapter.adapt(p.getLocation()));

            // Switch server.
            serverAPI.switchServer(PlayerAdapter.adapt(p), server);
            return true;
        }
        return false;
    }

    /**
     * Set the world to the location.
     *
     * @param region the region to get the world for
     * @param l      the location of the tpll
     */
    private void setWorldOfRegion(Region region, Location l) {
        // Check if the region is on the plot server.
        if (regionManager.isPlot(region)) {
            String location = plotSQL.getString("SELECT location FROM regions WHERE region='" + region.regionName() + "';");
            l.setWorld(WorldUtils.getWorld(location));
        } else {
            l.setWorld(WorldUtils.getWorld(constants.earthDimension()));
        }
    }

    /**
     * Get the altitude of the location
     *
     * @param p      the player
     * @param format the tpll format
     * @param l      the location of the tpll
     * @return {@link CompletableFuture<Double>} the completableFuture that will give the altitude
     */
    private CompletableFuture<Double> getAltitude(Player p, TpllFormat format, Location l) {

        // If the altitude was specified, return it.
        if (!Double.isNaN(format.getAltitude())) {
            return CompletableFuture.completedFuture(format.getAltitude());
        }

        // Get altitude from the dataset, this is used if the chunk is not yet generated,
        // or if we fail to get the altitude from the world safely.
        CompletableFuture<Double> datasetAltFuture;
        try {
            datasetAltFuture = new GeneratorDatasets(bteGeneratorSettings).<IScalarDataset>getCustom(EarthGeneratorPipelines.KEY_DATASET_HEIGHTS)
                    .getAsync(format.getCoordinates().getLng(), format.getCoordinates().getLat()).thenApply(a -> a + 1.0d);
        } catch (OutOfProjectionBoundsException e) { // out of bounds, notify user
            p.sendMessage(ChatUtils.error("These coordinates are out of the projection bounds."));
            return null;
        }

        // If the chunk is generated, get it from the world.
        // We use gen=false to avoid a deadlock if the chunk is currently being generated.
        // If the chunk is not loaded, we fall back to the dataset altitude.
        if (PaperLib.isChunkGenerated(l)) {
            return l.getWorld().getChunkAtAsync(l.getBlockX() >> 4, l.getBlockZ() >> 4, false).thenCompose(chunk -> {
                if (chunk != null) {
                    return CompletableFuture.completedFuture((double) Utils.getHighestYAt(l.getWorld(), l.getBlockX(), l.getBlockZ()));
                } else {
                    return datasetAltFuture;
                }
            });
        } else {
            p.sendMessage(ChatUtils.success("Location is generating, please wait a moment..."));
            return datasetAltFuture;
        }
    }

    /**
     * Teleport to the coordinates
     *
     * @param p         the player to teleport
     * @param altFuture the altitude future to get the altitude from
     * @param format    the format
     * @param l         the location to teleport to
     * @param fromEvent whether the command was executed from an event
     */
    private void teleport(Player p, CompletableFuture<Double> altFuture, TpllFormat format, Location l, boolean fromEvent) {
        altFuture.thenAccept(s -> Bukkit.getScheduler().runTask(instance, () -> {

            // If the tpll is from an event, don't save the previous coordinate, since that was already done when
            // creating the event.
            if (!fromEvent) {

                // Set current location for /back
                previousLocationTracker.setPreviousCoordinate(p.getUniqueId().toString(), LocationAdapter.adapt(p.getLocation()));
            }

            // Set the altitude
            l.setY(s);

            // Add tpll to statistics.
            Statistics.addTpll(globalSQL, p.getUniqueId().toString(), Time.getDate(Time.currentTime()));

            // Teleport player.
            PaperLib.teleportAsync(p, l);

            p.sendMessage(ChatUtils.success("Teleported to ").append(Component.text(DECIMAL_FORMATTER.format(format.getCoordinates().getLat()), NamedTextColor.DARK_AQUA))
                    .append(ChatUtils.success(", ")).append(Component.text(DECIMAL_FORMATTER.format(format.getCoordinates().getLng()), NamedTextColor.DARK_AQUA)));
        }));
    }

    @Override
    public String getLabel() {
        return "tpll";
    }

    @Override
    public String getDescription() {
        return "Teleport to coordinates";
    }
}

