package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.papercore.WorldUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Objects;

public class PlotsystemLocations extends NetworkRefreshableGui {

    private final PlotSQL plotSQL;
    private final GlobalSQL globalSQL;

    private int counter;

    public PlotsystemLocations(GuiProvider provider) {

        super(provider, 45, Component.text("Plotsystem Locations", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.plotSQL = provider.plotSQL();
        this.globalSQL = provider.globalSQL();
    }

    protected void createGui() {

        counter = 0;

        // Get plotsystem locations.
        ArrayList<String> locations = plotSQL.getStringList("SELECT name FROM location_data;");

        // Slot count.
        int slot = 10;

        // Make a button for each plot.
        for (String name : locations) {

            setItem(slot, Utils.createItem(nextIcon(), 1,
                            Utils.title(plotSQL.getString("SELECT alias FROM location_data WHERE name='" + name + "';"
                            )),
                            Utils.line("Click to teleport to the centre"),
                            Utils.line("of this plotsystem location.")),
                    (NetworkUser u) -> {

                        // Teleport to the centre of the plotsystem location.
                        // Get coordinate ids for min and max.
                        int min = plotSQL.getInt("SELECT coordMin FROM location_data WHERE name='" + name + "';");
                        int max = plotSQL.getInt("SELECT coordMax FROM location_data WHERE name='" + name + "';");

                        // Get middle points, adding the coordinate transformation.
                        double x = ((globalSQL.getDouble("SELECT x FROM coordinates WHERE id=" + max + ";") +
                                globalSQL.getDouble("SELECT x FROM coordinates WHERE id=" + min + ";")) / 2) +
                                plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + name + "';");

                        double z = ((globalSQL.getDouble("SELECT z FROM coordinates WHERE id=" + max + ";") +
                                globalSQL.getDouble("SELECT z FROM coordinates WHERE id=" + min + ";")) / 2) +
                                plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + name + "';");

                        String server = plotSQL.getString("SELECT server FROM location_data WHERE name='" + name +
                                "';");

                        NetworkLocation location = LocationAdapter.adapt(u.player.getLocation());
                        if (server.equals(provider.constants().serverName())) {

                            u.player.closeInventory();

                            // Teleport to the location.
                            World world = WorldUtils.getWorld(name);
                            double y = Objects.requireNonNull(world).getHighestBlockYAt((int) x, (int) z);
                            y++;

                            provider.eventAPI().createTeleportEvent(false, u.player.getUniqueId().toString(),
                                    "teleport " + name + " " + x + " " + y + " " + z + " "
                                            + u.player.getLocation().getYaw() + " " + u.player.getLocation().getPitch(),
                                    "&aTeleported to location &3" + plotSQL.getString("SELECT alias FROM " +
                                            "location_data WHERE name='" + name + "';"), location);
                        } else {

                            // Set the server join event.
                            provider.eventAPI().createTeleportEvent(true, u.player.getUniqueId().toString(),
                                    "teleport " + name + " " + x + " " + z + " "
                                            + u.player.getLocation().getYaw() + " " + u.player.getLocation().getPitch(),
                                    "&aTeleported to location &3" + plotSQL.getString("SELECT alias FROM " +
                                            "location_data WHERE name='" + name + "';"), location);

                            // Teleport them to another server.
                            u.player.closeInventory();
                            provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), server);
                        }
                    });

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
                        Utils.line("Open the building menu.")),
                (NetworkUser u) -> {

                    // Delete this gui.
                    this.delete();

                    // Switch to plot info.
                    u.mainGui = new BuildGui(provider, u);
                    u.mainGui.open(u.player);
                });
    }

    private Material nextIcon() {

        Material mat;

        switch (counter) {

            case 1 -> mat = Material.SPRUCE_BOAT;
            case 2 -> mat = Material.BIRCH_BOAT;
            case 3 -> mat = Material.JUNGLE_BOAT;
            case 4 -> mat = Material.ACACIA_BOAT;
            case 5 -> mat = Material.DARK_OAK_BOAT;
            default -> mat = Material.OAK_BOAT;
        }

        if (counter == 5) {
            counter = 0;
        } else {
            counter++;
        }

        return mat;
    }
}
