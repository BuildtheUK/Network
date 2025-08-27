package net.bteuk.network.gui.regions;

import net.bteuk.network.Network;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.eventing.listeners.regions.RegionTagListener;
import net.bteuk.network.gui.InviteMembers;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import static net.bteuk.network.utils.Constants.SERVER_NAME;

public class RegionInfo extends NetworkRefreshableGui {

    private final Region region;
    private final String uuid;

    private final GlobalSQL globalSQL;

    private RegionTagListener regionTagListener;

    private boolean actionActive = false;

    public RegionInfo(Region region, String uuid) {

        super(27, Component.text("Region " + region.getTag(uuid), NamedTextColor.AQUA, TextDecoration.BOLD));

        this.region = region;
        this.uuid = uuid;

        globalSQL = Network.getInstance().getGlobalSQL();

        createGui();
    }

    private void createGui() {

        // Region info.
        // If region has tag set then show both name and tag.
        if (region.regionName().equals(region.getTag(uuid))) {
            setItem(4, Utils.createItem(Material.BOOK, 1,
                    Utils.title("Region " + region.regionName()),
                    Utils.line("Region Owner ")
                            .append(Component.text(region.ownerName(), NamedTextColor.GRAY)),
                    Utils.line("Region Members ")
                            .append(Component.text(region.memberCount(), NamedTextColor.GRAY))));
        } else {
            setItem(4, Utils.createItem(Material.BOOK, 1,
                    Utils.title("Region " + region.regionName()),
                    Utils.line("Region Tag ")
                            .append(Component.text(region.getTag(uuid), NamedTextColor.GRAY)),
                    Utils.line("Region Owner ")
                            .append(Component.text(region.ownerName(), NamedTextColor.GRAY)),
                    Utils.line("Region Members ")
                            .append(Component.text(region.memberCount(), NamedTextColor.GRAY))));
        }

        // Leave Region.
        setItem(8, Utils.createItem(Material.RED_CONCRETE, 1,
                        Utils.title("Leave Region")),
                (NetworkUser u) -> {

                    // Send leave event to server events.
                    EventManager.createEvent(u.player.getUniqueId().toString(), "network", globalSQL.getString(
                                    "SELECT name FROM server_data WHERE type='EARTH';"),
                            "region leave " + region.regionName(),
                            "&aYou have left region &3" + region.getTag(u.player.getUniqueId().toString()));

                    // Return to region menu and close inventory.
                    u.player.closeInventory();
                    this.delete();

                    u.mainGui = new RegionMenu(u);
                });

        // Set region tag.
        setItem(20, Utils.createItem(Material.WRITABLE_BOOK, 1,
                        Utils.title("Set Region Tag"),
                        Utils.line("Click to give this region a custom name."),
                        Utils.line("You will be prompted to type a name in chat."),
                        Utils.line("It can have a maximum of 64 characters.")),

                (NetworkUser u) -> {

                    // Create chat listener and send message telling the player.
                    // Listener will automatically close after 1 minute or if a message is sent.
                    if (regionTagListener != null) {
                        regionTagListener.unregister();
                    }

                    // Create chat listener and send message telling the player.
                    // Listener will automatically close after 1 minute or if a message is sent.
                    regionTagListener = new RegionTagListener(u.player, region);
                    u.player.sendMessage(ChatUtils.success("Write your region tag in chat, the first message counts."));
                    u.player.closeInventory();
                });

        setItem(21, enchant(Utils.createItem(Material.LIGHTNING_ROD, 1,
                        region.isPinned(uuid) ? Utils.title("Unpin Region") : Utils.title("Pin Region"),
                        Utils.line("A pinned region will always show"),
                        Utils.line("at the start of the region menu."))),
                user -> {
                    if (!actionActive) {
                        actionActive = true;

                        try {
                            region.setPinned(uuid, !region.isPinned(uuid));
                        } finally {
                            this.refresh();
                        }
                    }
                });

        // Teleport to region.
        setItem(23, Utils.createItem(Material.ENDER_PEARL, 1,
                        Utils.title("Teleport to Region"),
                        Utils.line("Teleports you to the region at the"),
                        Utils.line("current set location."),
                        Utils.line("You can edit the location by clicking on the"),
                        Utils.line("'Set Location' button while standing in the region.")),
                (NetworkUser u) -> {

                    // If the player is on the earth server get the coordinate.
                    if (SERVER_NAME.equals(globalSQL.getString("SELECT name FROM server_data WHERE type='EARTH';"))) {

                        // Close inventory.
                        u.player.closeInventory();

                        // Set current location for /back
                        Back.setPreviousCoordinate(u.player.getUniqueId().toString(), u.player.getLocation());

                        Location l = globalSQL.getLocation(region.getCoordinateID(uuid));
                        u.player.teleport(l);
                        u.player.sendMessage(ChatUtils.success("Teleported to region ")
                                .append(Component.text(region.getTag(uuid), NamedTextColor.DARK_AQUA)));
                    } else {

                        // Create teleport region event.
                        EventManager.createTeleportEvent(true, u.player.getUniqueId().toString(), "network",
                                "teleport region " + region.regionName(), u.player.getLocation());

                        // Switch server.
                        SwitchServer.switchServer(u.player, globalSQL.getString("SELECT name FROM server_data WHERE " +
                                "type='EARTH'"));
                    }
                });

        // Set teleport location.
        setItem(24, Utils.createItem(Material.ENDER_EYE, 1,
                        Utils.title("Set Location"),
                        Utils.line("Sets the teleport location of this region"),
                        Utils.line("to you current location."),
                        Utils.line("You must be standing in the region"),
                        Utils.line("for this to work.")),
                (NetworkUser u) -> {

                    u.player.closeInventory();

                    // Check if the player is in the correct region.
                    if (u.inRegion) {
                        if (u.region.equals(region)) {

                            // Update the previous coordinate.
                            int coordinateID = region.getCoordinateID(uuid);
                            globalSQL.updateCoordinate(coordinateID, u.player.getLocation());

                            // Create coordinate id for location of player and set that as the new coordinate id.
                            region.setCoordinateID(uuid, coordinateID);
                            u.player.sendMessage(ChatUtils.success("Set teleport location for region ")
                                    .append(Component.text(region.getTag(uuid), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" at your current location.")));
                        } else {
                            u.player.sendMessage(ChatUtils.error("You are not standing in the correct region."));
                        }
                    } else {
                        u.player.sendMessage(ChatUtils.error("You are not standing in a region."));
                    }
                });

        // Owner only settings.
        if (region.isOwner(uuid)) {

            // If region is private, make public button, if public make private button.
            if (region.status() == RegionStatus.PUBLIC) {
                setItem(0, Utils.createItem(Material.IRON_TRAPDOOR, 1,
                                Utils.title("Make Private"),
                                Utils.line("New members will need your approval to join the region.")),
                        (NetworkUser u) -> {

                            // Set the region as private and refresh gui.
                            region.setDefault();

                            u.player.sendMessage(ChatUtils.success("Region ")
                                    .append(Component.text(region.getTag(uuid), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" is now private.")));
                            this.refresh();
                        });
            } else {
                setItem(0, Utils.createItem(Material.OAK_TRAPDOOR, 1,
                                Utils.title("Make Public"),
                                Utils.line("New members can join the region without approval.")),
                        (NetworkUser u) -> {

                            // Set the region as public and refresh gui.
                            region.setPublic();

                            // Approve any active region requests for this region.
                            // Make sure this is done on the correct server.
                            // Create teleport region event.
                            if (region.hasRequests()) {
                                globalSQL.update(
                                        "INSERT INTO server_events(uuid,type,server,event) VALUES('" + u.player.getUniqueId() + "','network'," +
                                                globalSQL.getString(
                                                        "SELECT name FROM server_data WHERE type='EARTH';") +
                                                "'region request accept "
                                                + region.regionName() + "');");
                            }

                            u.player.sendMessage(ChatUtils.success("Region ")
                                    .append(Component.text(region.getTag(uuid), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" is now public.")));
                            this.refresh();
                        });
            }

            // Invite member.
            setItem(9, Utils.createItem(Material.OAK_BOAT, 1,
                            Utils.title("Invite Members"),
                            Utils.line("Invite a new member to your region."),
                            Utils.line("You can only invite online users.")),
                    (NetworkUser u) -> {

                        // Open the invite member menu.
                        this.delete();

                        u.mainGui = new InviteMembers(region, RegionType.REGION);
                        u.mainGui.open(u.player);
                    });

            // Manage members.
            setItem(18, Utils.createItem(Material.PLAYER_HEAD, 1,
                            Utils.title("Region Members"),
                            Utils.line("Manage the members in your region.")),
                    (NetworkUser u) -> {

                        // Open the invite member menu.
                        this.delete();

                        u.mainGui = new RegionMembers(region);
                        u.mainGui.open(u.player);
                    });
        }

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the region menu.")),
                (NetworkUser u) -> {

                    // Delete this gui.
                    this.delete();

                    // Switch to plot info.
                    u.mainGui = new RegionMenu(u);
                    u.mainGui.open(u.player);
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
        actionActive = false;
    }

    private ItemStack enchant(ItemStack itemStack) {
        if (region.isPinned(uuid)) {
            Utils.enchant(itemStack);
        }
        return itemStack;
    }
}
