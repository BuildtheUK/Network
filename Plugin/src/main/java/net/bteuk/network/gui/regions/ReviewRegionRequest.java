package net.bteuk.network.gui.regions;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.regions.Request;
import net.bteuk.network.regions.sql.RegionSQL;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

public class ReviewRegionRequest extends NetworkRefreshableGui {

    private final RegionSQL regionSQL;
    private final Request request;
    private final boolean staff;

    public ReviewRegionRequest(GuiProvider provider, Request request, boolean staff) {

        super(provider, 27, Component.text("Review Region Request", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.regionSQL = provider.regionSQL();
        this.request = request;
        this.staff = staff;
    }

    protected void createGui() {

        setItem(4, Utils.createItem(Material.BOOK, 1, Utils.title("Region " + request.region), Utils.line("Requested by ")
                .append(Component.text(provider.globalSQL().getString("SELECT name FROM " + "player_data WHERE uuid='" + request.uuid + "';"), NamedTextColor.GRAY))));

        setItem(11, Utils.createItem(Material.LIME_CONCRETE, 1, Utils.title("Accept Request"), Utils.line("The user will be able to build in this region.")), (NetworkUser u) -> {

            // Create event to accept request.
            provider.eventAPI().createEvent(u.player.getUniqueId().toString(), provider.globalSQL().getString("SELECT name FROM server_data WHERE " + "type='EARTH';"),
                    "region request accept " + request.region + " " + request.uuid);

            // Return to the request menu.
            this.delete();

            if (staff) {

                u.staffGui = null;

                // Delay opening to make sure request was dealt with.
                Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                    u.staffGui = new ReviewRegionRequests(provider, true, u.player.getUniqueId().toString());
                    u.staffGui.open(u.player);
                }, 20L);
            } else {

                u.mainGui = null;

                // Delay opening to make sure request was dealt with.
                Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                    u.mainGui = new ReviewRegionRequests(provider, false, u.player.getUniqueId().toString());
                    u.mainGui.open(u.player);
                }, 20L);
            }
        });

        setItem(15, Utils.createItem(Material.RED_CONCRETE, 1, Utils.title("Deny Request"), Utils.line("The user will not be able to build in this region.")), (NetworkUser u) ->

        {

            // Create event to deny request.
            provider.eventAPI().createEvent(u.player.getUniqueId().toString(), provider.globalSQL().getString("SELECT name FROM server_data WHERE " + "type='EARTH';"),
                    "region request deny " + request.region + " " + request.uuid);

            // Return to the request menu.
            this.delete();

            if (staff) {

                u.staffGui = null;

                // Delay opening to make sure request was dealt with.
                Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                    u.staffGui = new ReviewRegionRequests(provider, true, u.player.getUniqueId().toString());
                    u.staffGui.open(u.player);
                }, 20L);
            } else {

                u.mainGui = null;

                // Delay opening to make sure request was dealt with.
                Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                    u.mainGui = new ReviewRegionRequests(provider, false, u.player.getUniqueId().toString());
                    u.mainGui.open(u.player);
                }, 20L);
            }
        });

        setItem(22, Utils.createItem(Material.ENDER_PEARL, 1, Utils.title("Teleport to Region"), Utils.line("Teleport to the location where the request was made.")),
                (NetworkUser u) ->

                {

                    GlobalSQL globalSQL = provider.globalSQL();

                    // Get coordinate.
                    Location l = globalSQL.getLocation(
                            regionSQL.getInt("SELECT coordinate_id FROM region_requests " + "WHERE region='" + request.region + "' AND uuid='" + request.uuid + "';"));

                    // If the player is on the earth server get the coordinate.
                    if (provider.constants().serverName().equals(globalSQL.getString("SELECT name FROM server_data WHERE type='EARTH';"))) {

                        // Close inventory.
                        u.player.closeInventory();

                        // Set current location for /back
                        provider.back().setPreviousCoordinate(u.player.getUniqueId().toString(), LocationAdapter.adapt(u.player.getLocation()));

                        // Teleport player.
                        u.player.teleport(l);
                        u.player.sendMessage(ChatUtils.success("Teleported to region ").append(Component.text(request.region, NamedTextColor.DARK_AQUA)));
                    } else {
                        u.player.closeInventory();

                        // Create teleport event.
                        provider.eventAPI().createTeleportEvent(true, u.player.getUniqueId().toString(), "teleport " + provider.constants().earthWorld() + " " + l.getX() + " " + l.getZ() + " " + l.getYaw() + " " + l.getPitch(),
                                LocationAdapter.adapt(u.player.getLocation()));

                        // Switch server.
                        provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), globalSQL.getString("SELECT name FROM server_data WHERE " + "type='EARTH'"));
                    }
                });

        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Previous Page"), Utils.line("Open the region request menu.")), (NetworkUser u) ->

        {

            // Return to the request menu.
            this.delete();

            if (staff) {

                u.staffGui = null;

                // Delay opening to make sure the request was dealt with.
                Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                    u.staffGui = new ReviewRegionRequests(provider, true, u.player.getUniqueId().toString());
                    u.staffGui.open(u.player);
                }, 20L);
            } else {

                u.mainGui = null;

                // Delay opening to make sure the request was dealt with.
                Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                    u.mainGui = new ReviewRegionRequests(provider, false, u.player.getUniqueId().toString());
                    u.mainGui.open(u.player);
                }, 20L);
            }
        });
    }
}
