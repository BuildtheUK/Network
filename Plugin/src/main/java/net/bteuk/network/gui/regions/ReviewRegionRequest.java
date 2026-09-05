package net.bteuk.network.gui.regions;

import net.bteuk.network.api.entity.Role;
import net.bteuk.network.eventing.listeners.SingleMessageChatListener;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.regions.Request;
import net.bteuk.network.regions.sql.RegionSQL;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.btuk.network.lib.dto.RegionRequestEvent;
import org.btuk.network.lib.enums.ApprovalAction;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class ReviewRegionRequest extends NetworkRefreshableGui {

    private final JavaPlugin plugin;
    private final Roles roles;
    private final RegionSQL regionSQL;
    private final Request request;
    private final boolean staff;
    private final MessageSender messageSender;

    private Role role;

    public ReviewRegionRequest(GuiProvider provider, Request request, boolean staff) {

        super(provider, 27, Component.text("Review Region Request", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.plugin = provider.instance();
        this.roles = provider.roles();
        this.regionSQL = provider.regionSQL();
        this.request = request;
        this.staff = staff;
        this.messageSender = provider.messageSender();
    }

    @Override
    protected void loadData() {
        String roleId = roles.getBuilderRole(request.region).join();
        this.role = roles.getRoleById(roleId);
    }

    protected void createGui() {

        setItem(4, Utils.createItem(Material.BOOK, 1, Utils.title("Region " + request.region),
                Utils.line("Requested by ")
                        .append(Component.text(provider.globalSQL().getString("SELECT name FROM " + "player_data WHERE uuid='" + request.uuid + "';"), NamedTextColor.GRAY)),
                ChatUtils.line("Builder role: ").append(role.getColouredRoleName())));

        setItem(11, Utils.createItem(Material.LIME_CONCRETE, 1, Utils.title("Accept Request"), Utils.line("The user will be able to build in this region.")),
                (NetworkUser user) -> {
                    // Send the event to the proxy, it will handle the routing the correct server.
                    RegionRequestEvent event = new RegionRequestEvent(request.region, user.getUuid(), request.uuid, staff, ApprovalAction.ACCEPT, null);
                    messageSender.sendSocketMessage(event);

                    // Return to the request menu.
                    returnToRequests(user);
                });

        setItem(15, Utils.createItem(Material.RED_CONCRETE, 1, Utils.title("Deny Request"), Utils.line("The user will not be able to build in this region.")),
                (NetworkUser user) -> {
                    // For staff requests a reason must be provided, start a chat listener.
                    if (staff) {
                        new SingleMessageChatListener(plugin, user.player, ChatUtils.error("Region request reason prompt has timed out."), 1200L, chatEvent -> {
                            if (chatEvent.getPlayer() != user.player) {
                                return false;
                            }

                            chatEvent.setCancelled(true);
                            RegionRequestEvent event = new RegionRequestEvent(request.region, user.getUuid(), request.uuid, true, ApprovalAction.REJECT,
                                    PlainTextComponentSerializer.plainText().serialize(chatEvent.message()));
                            messageSender.sendSocketMessage(event);

                            returnToRequests(user);
                            return true;
                        });

                        user.staffGui = new ReviewRegionRequests(provider, true, user.player.getUniqueId().toString());
                        user.player.closeInventory();
                        user.sendMessage(ChatUtils.error("Please write a reason for denying the request in chat, the first message will be used."));
                    } else {
                        RegionRequestEvent event = new RegionRequestEvent(request.region, user.getUuid(), request.uuid, false, ApprovalAction.REJECT, null);
                        messageSender.sendSocketMessage(event);

                        // Return to the request menu.
                        returnToRequests(user);
                    }
                });

        setItem(22, Utils.createItem(Material.ENDER_PEARL, 1, Utils.title("Teleport to Region"), Utils.line("Teleport to the location where the request was made.")),
                (NetworkUser u) -> {

                    GlobalSQL globalSQL = provider.globalSQL();

                    // Get coordinate.
                    Location l = globalSQL.getLocation(
                            regionSQL.getInt("SELECT coordinate_id FROM region_requests " + "WHERE region='" + request.region + "' AND uuid='" + request.uuid + "';"));

                    // If the player is on the earth server get the coordinate.
                    if (provider.constants().serverName().equals(globalSQL.getString("SELECT name FROM server_data WHERE type='EARTH';"))) {

                        // Close inventory.
                        u.player.closeInventory();

                        // Set current location for /back
                        provider.previousLocationTracker().setPreviousCoordinate(u.player.getUniqueId().toString(), LocationAdapter.adapt(u.player.getLocation()));

                        // Teleport player.
                        u.player.teleport(l);
                        u.player.sendMessage(ChatUtils.success("Teleported to region ").append(Component.text(request.region, NamedTextColor.DARK_AQUA)));
                    } else {
                        u.player.closeInventory();

                        // Create teleport event.
                        provider.eventAPI().createTeleportEvent(true, u.player.getUniqueId().toString(),
                                "teleport " + provider.constants().earthDimension() + " " + l.getX() + " " + l.getZ() + " " + l.getYaw() + " " + l.getPitch(),
                                LocationAdapter.adapt(u.player.getLocation()));

                        // Switch server.
                        provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), globalSQL.getString("SELECT name FROM server_data WHERE " + "type='EARTH'"));
                    }
                });

        // Return to the request menu.
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Previous Page"), Utils.line("Open the region request menu.")), this::returnToRequests);
    }

    private void returnToRequests(NetworkUser user) {
        // Return to the request menu.
        this.delete();

        if (staff) {

            user.staffGui = null;

            // Delay opening to make sure the request was dealt with.
            Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                user.staffGui = new ReviewRegionRequests(provider, true, user.player.getUniqueId().toString());
                user.staffGui.open(user.player);
            }, 20L);
        } else {

            user.mainGui = null;

            // Delay opening to make sure the request was dealt with.
            Bukkit.getScheduler().runTaskLater(provider.instance(), () -> {
                user.mainGui = new ReviewRegionRequests(provider, false, user.player.getUniqueId().toString());
                user.mainGui.open(user.player);
            }, 20L);
        }
    }
}
