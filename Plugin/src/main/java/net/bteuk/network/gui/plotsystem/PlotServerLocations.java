package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.minecraft.gui.GuiFactory;
import org.btuk.minecraft.gui.GuiOpenContext;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;

public class PlotServerLocations extends NetworkRefreshableGui {

    private final PlotSQL plotSQL;
    private final Roles roles;
    private int plotDifficulty;
    private Material mDifficulty;
    private String sDifficulty;
    private int plotSize;
    private Material mSize;
    private String sSize;

    public PlotServerLocations(GuiProvider provider, NetworkUser u) {

        super(provider, 45, Component.text("Plot Locations", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.plotSQL = provider.plotSQL();
        this.roles = provider.roles();

        // Set default values of gui.

        // Default difficulty will depend on the role of the player.
        // If the player is a guest, default will be easy, if apprentice default will be normal and if jrbuilder
        // default will be hard.
        // All other roles will have default set to random.
        if (u.player.hasPermission("uknet.plots.suggested.all")) {
            plotDifficulty = 0;
        } else if (u.player.hasPermission("uknet.plots.suggested.easy")) {
            plotDifficulty = 1;
        } else if (u.player.hasPermission("uknet.plots.suggested.normal")) {
            plotDifficulty = 2;
        } else if (u.player.hasPermission("uknet.plots.suggested.hard")) {
            plotDifficulty = 3;
        } else {
            plotDifficulty = 0;
        }
        setDifficulty();

        plotSize = 0;
        mSize = Material.GRAY_CONCRETE;
        sSize = "Random";
    }

    private void setDifficulty() {
        if (plotDifficulty == 1) {
            mDifficulty = Material.LIME_CONCRETE;
            sDifficulty = "Easy";
        } else if (plotDifficulty == 2) {
            mDifficulty = Material.YELLOW_CONCRETE;
            sDifficulty = "Normal";
        } else if (plotDifficulty == 3) {
            mDifficulty = Material.RED_CONCRETE;
            sDifficulty = "Hard";
        } else {
            mDifficulty = Material.GRAY_CONCRETE;
            sDifficulty = "Random";
        }
    }

    private void setSize() {
        if (plotSize == 1) {
            mSize = Material.LIME_CONCRETE;
            sSize = "Small";
        } else if (plotSize == 2) {
            mSize = Material.YELLOW_CONCRETE;
            sSize = "Medium";
        } else if (plotSize == 3) {
            mSize = Material.RED_CONCRETE;
            sSize = "Large";
        } else {
            mSize = Material.GRAY_CONCRETE;
            sSize = "Random";
        }
    }

    protected void createGui() {

        setDifficulty();

        setSize();

        // Select plot difficulty.
        setItem(3, Utils.createItem(mDifficulty, 1, Utils.title(sDifficulty), Utils.line("Click to toggle the difficulty."), Utils.line("You will only be teleported to"),
                Utils.line("plots of the selected difficulty.")), (NetworkUser u) -> {

            // Update the difficulty.
            plotDifficulty = (plotDifficulty == 3) ? 0 : plotDifficulty + 1;

            // Update the gui.
            this.refresh();
            this.updatePlayerInventory(u.player);
        });

        // Select plot size.
        setItem(5, Utils.createItem(mSize, 1, Utils.title(sSize), Utils.line("Click to toggle the size."), Utils.line("You will only be teleported to"),
                Utils.line("plots of the selected size.")), (NetworkUser u) ->

        {

            // Update the Size.
            plotSize = (plotSize == 3) ? 0 : plotSize + 1;

            // Update the gui.
            this.refresh();
            this.updatePlayerInventory(u.player);
        });

        // Get all locations from database.
        ArrayList<String> locations = plotSQL.getStringList("SELECT name FROM location_data");

        // Starting slot.
        int slot = 10;

        // Iterate through locations and add them to the gui.
        for (String location : locations) {

            // Create location button.
            setItem(slot, Utils.createItem(Material.DIAMOND_PICKAXE, 1, Utils.title(plotSQL.getString("SELECT alias FROM location_data WHERE name='" + location + "';")),
                    Utils.line("Click to teleport to a plot in this location"), Utils.line("subject to the settings shown above."),
                    Utils.line("Available plots of each difficulty:"), Utils.line("Easy: ").append(Component.text(
                            plotSQL.getInt("SELECT count(id) FROM plot_data WHERE " + "location='" + location + "' AND status='unclaimed' AND " + "difficulty=1;"),
                            NamedTextColor.GRAY)), Utils.line("Normal: ").append(Component.text(
                            plotSQL.getInt("SELECT count(id) FROM plot_data WHERE " + "location='" + location + "' AND status='unclaimed' AND " + "difficulty=2;"),
                            NamedTextColor.GRAY)), Utils.line("Hard: ").append(Component.text(
                            plotSQL.getInt("SELECT count(id) FROM plot_data WHERE " + "location='" + location + "' AND status='unclaimed' AND " + "difficulty=3;"),
                            NamedTextColor.GRAY))), (NetworkUser u) ->

            {

                // Check if a plot is available with the given parameters.
                // If difficulty and size are 0 pick a random plot within the parameters that is allowed for
                // the player.
                int id;

                if (plotDifficulty == 0 && plotSize == 0) {

                    if (roles.builderRole(u.player).getId().equals("jrbuilder")) {

                        // Select a random plot of the hard difficulty.
                        // Since this is the next plot difficulty to get Builder.
                        id = plotSQL.getInt("SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND difficulty=3 ORDER BY RAND() LIMIT 1;");
                    } else if (roles.builderRole(u.player).getId().equals("apprentice")) {

                        // Select a random plot of the normal difficulty.
                        // Since this is the next plot difficulty to get Jr.Builder.
                        id = plotSQL.getInt("SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND difficulty=2 ORDER BY RAND() LIMIT 1;");
                    } else if (roles.builderRole(u.player).getId().equals("default")) {

                        // Select a random plot of the easy difficulty.
                        // Since this is the next plot difficulty to get Apprentice.
                        id = plotSQL.getInt("SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND difficulty=1 ORDER BY RAND() LIMIT 1;");
                    } else {

                        // Select a random plot of any difficulty.
                        id = plotSQL.getInt("SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' ORDER BY RAND() LIMIT 1;");
                    }
                } else if (plotDifficulty == 0) {
                    // Pick plot with random difficulty but fixed size.

                    if (roles.builderRole(u.player).getId().equals("jrbuilder")) {

                        // Select a random plot of the hard difficulty.
                        // Since this is the next plot difficulty to get Builder.
                        id = plotSQL.getInt(
                                "SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND difficulty=3 AND size=" + plotSize + " ORDER BY" + " " + "RAND() LIMIT 1;");
                    } else if (roles.builderRole(u.player).getId().equals("apprentice")) {

                        // Select a random plot of the normal difficulty.
                        // Since this is the next plot difficulty to get Jr.Builder.
                        id = plotSQL.getInt(
                                "SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND difficulty=2 AND size=" + plotSize + " ORDER BY" + " " + "RAND() LIMIT 1;");
                    } else if (roles.builderRole(u.player).getId().equals("default")) {

                        // Select a random plot of the easy difficulty.
                        // Since this is the next plot difficulty to get Apprentice.
                        id = plotSQL.getInt(
                                "SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND difficulty=1 AND size=" + plotSize + " ORDER BY" + " " + "RAND() LIMIT 1;");
                    } else {
                        // Select a random plot of any difficulty.
                        id = plotSQL.getInt(
                                "SELECT id FROM plot_data WHERE location = '" + location + "' AND status='unclaimed' AND size=" + plotSize + " ORDER BY RAND() LIMIT 1;");
                    }
                } else if (plotSize == 0) {
                    // Pick plot with random size but fixed difficulty.

                    id = plotSQL.getInt(
                            "SELECT id FROM plot_data WHERE location='" + location + "' AND status='unclaimed' AND difficulty=" + plotDifficulty + " ORDER BY RAND() " + "LIMIT " + "1;");
                } else {
                    // Both size and difficulty are specified.

                    // Select a random plot of any difficulty.
                    id = plotSQL.getInt(
                            "SELECT id FROM plot_data WHERE location='" + location + "' AND status='unclaimed' AND difficulty=" + plotDifficulty + " AND size=" + plotSize + " " + "ORDER BY RAND() LIMIT 1;");
                }

                // If no plots fit the specified parameters the id will be 0.
                if (id == 0) {

                    u.player.sendMessage(ChatUtils.error("No plots are available with the specified settings,"));
                    u.player.sendMessage(ChatUtils.error("try another location or change the settings."));
                } else {

                    // Get the server of the plot.
                    String server = plotSQL.getString(
                            "SELECT server FROM location_data WHERE name='" + plotSQL.getString("SELECT location FROM plot_data WHERE id=" + id + ";") + "';");

                    // If the plot is on the current server teleport them directly.
                    // Else teleport them to the correct server and then teleport them to the plot.
                    if (server.equals(provider.constants().serverName())) {

                        u.player.closeInventory();

                        provider.eventAPI().createTeleportEvent(false, u.player.getUniqueId().toString(), "plotsystemteleport plot " + id, LocationAdapter.adapt(u.player.getLocation()));
                    } else {
                        u.player.closeInventory();

                        // Set the server join event.
                        provider.eventAPI().createTeleportEvent(true, u.player.getUniqueId().toString(), "plotsystemteleport plot " + id, LocationAdapter.adapt(u.player.getLocation()));

                        // Teleport them to another server.
                        provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), server);
                    }
                }
            });

            // Increase the slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, basically add 3.
                slot += 3;
            } else if (slot == 34) {
                // Last possible slot, end iteration.
                break;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        // Return
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the build menu.")), (NetworkUser u) -> {
            GuiFactory guiFactory = getManager().getReturnGui(PlotSystemGuiType.PLOT_SERVER_LOCATIONS);
            if (guiFactory != null) {
                // Switch back to the build menu.
                this.delete();
                u.mainGui = guiFactory.create(new GuiOpenContext(u.player, Collections.emptyMap()));
                u.mainGui.open(u.player);
            }
        });
    }
}
