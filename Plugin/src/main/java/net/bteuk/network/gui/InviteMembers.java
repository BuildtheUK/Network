package net.bteuk.network.gui;

import net.bteuk.network.Network;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.gui.plotsystem.PlotInfo;
import net.bteuk.network.gui.plotsystem.ZoneInfo;
import net.bteuk.network.gui.regions.RegionInfo;
import net.bteuk.network.lib.dto.OnlineUser;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.sql.RegionSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.RegionType;
import net.bteuk.network.utils.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;

public class InviteMembers extends Gui {

    // This is either a region, plot or zone.
    // We can decypher it by using the regionType variable.
    private final Object o;
    private final RegionType regionType;
    private final GlobalSQL globalSQL;
    private final RegionSQL regionSQL;
    private final PlotSQL plotSQL;
    private int page;

    public InviteMembers(Object o, RegionType regionType) {

        super(45, Component.text("Invite Members", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.o = o;
        this.regionType = regionType;

        page = 1;

        globalSQL = Network.getInstance().getGlobalSQL();
        regionSQL = Network.getInstance().regionSQL;
        plotSQL = Network.getInstance().getPlotSQL();

        createGui();
    }

    private void createGui() {

        // Get all online players in the network.
        List<String> online_users = Network.getInstance().getOnlineUsers().stream().map(OnlineUser::getUuid).toList();

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // If page is greater than 1 add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1,
                            Utils.title("Previous Page"),
                            Utils.line("Open the previous page of online users.")),
                    u ->

                    {

                        // Update the gui.
                        page--;
                        this.refresh();
                        u.player.getOpenInventory().getTopInventory().setContents(this.getInventory().getContents());
                    });
        }

        // Iterate through all online players.
        for (String uuid : online_users) {

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1,
                                Utils.title("Next Page"),
                                Utils.line("Open the next page of online users.")),
                        u ->

                        {

                            // Update the gui.
                            page++;
                            this.refresh();
                            u.player.getOpenInventory().getTopInventory()
                                    .setContents(this.getInventory().getContents());
                        });

                // Stop iterating.
                break;
            }

            // Check whether the player is not already the owner or member of the region, plot or zone, if true skip
            // them.
            if (isMember(uuid)) {
                continue;
            }

            // If skip is greater than 0, skip this iteration.
            if (skip > 0) {
                skip--;
                continue;
            }

            // Add player to gui.
            setItem(slot, Utils.createPlayerSkull(uuid, 1,
                            Utils.title("Invite " + globalSQL.getString(
                                    "SELECT name FROM player_data WHERE uuid='" + uuid + "';") + " to your " + regionType.label + "."),
                            Utils.line("They will receive an invitation in chat.")),
                    u ->

                    {

                        // Check if the player is still online.
                        if (Network.getInstance().isOnlineOnNetwork(uuid)) {

                            // Check if the player is not already a member of the region, plot or zone.
                            if (!isMember(uuid)) {

                                // Check if the player has not already been invited.
                                if (!isInvited(uuid)) {

                                    invite(u, uuid);
                                } else {
                                    u.player.sendMessage(ChatUtils.error("You've already invited this player to your "
                                            + regionType.label + "."));
                                }
                            } else {
                                u.player.sendMessage(ChatUtils.error(
                                        "This player is already a member of your " + regionType.label + "."));
                            }
                        } else {
                            u.player.sendMessage(ChatUtils.error("This player is no longer online."));
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

        // Return to region, plot or zone info.
        if (regionType == RegionType.REGION) {

            Region region = (Region) o;

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                            Utils.title("Return"),
                            Utils.line("Return to the menu of region ")
                                    .append(Component.text(region.getTag(region.getOwner()), NamedTextColor.GRAY))),
                    u ->

                    {

                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch back to plot info.
                        u.mainGui = new RegionInfo(region, u.player.getUniqueId().toString());
                        u.mainGui.open(u.player);
                    });
        } else if (regionType == RegionType.PLOT) {

            Integer plotID = (Integer) o;

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                            Utils.title("Return"),
                            Utils.line("Return to the menu of plot " + plotID + ".")),
                    u ->

                    {

                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch back to plot info.
                        u.mainGui = new PlotInfo(u, plotID);
                        u.mainGui.open(u.player);
                    });
        } else if (regionType == RegionType.ZONE) {

            Integer zoneID = (Integer) o;

            setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                            Utils.title("Return"),
                            Utils.line("Return to the menu of zone " + zoneID + ".")),
                    u ->

                    {

                        // Delete this gui.
                        this.delete();
                        u.mainGui = null;

                        // Switch back to plot info.
                        u.mainGui = new ZoneInfo(u, zoneID, u.player.getUniqueId().toString());
                        u.mainGui.open(u.player);
                    });
        }
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }

    public boolean isInvited(String uuid) {

        // Check whether the player is not already the owner or member of the region, plot or zone, if true skip them.
        if (regionType == RegionType.REGION) {

            Region region = (Region) o;

            return regionSQL.hasRow("SELECT region FROM region_invites WHERE region='" + region.regionName() + "' AND" +
                    " uuid='" + uuid + "';");
        } else if (regionType == RegionType.PLOT) {

            Integer plotID = (Integer) o;

            return plotSQL.hasRow("SELECT id FROM plot_invites WHERE id='" + plotID + "' AND uuid='" + uuid + "';");
        } else if (regionType == RegionType.ZONE) {

            Integer zoneID = (Integer) o;

            return plotSQL.hasRow("SELECT id FROM zone_invites WHERE id='" + zoneID + "' AND uuid='" + uuid + "';");
        }

        return false;
    }

    public boolean isMember(String uuid) {

        if (regionType == RegionType.REGION) {

            Region region = (Region) o;

            return regionSQL.hasRow("SELECT uuid FROM region_members WHERE region='" + region.regionName() + "' AND " +
                    "uuid='" + uuid + "';");
        } else if (regionType == RegionType.PLOT) {

            Integer plotID = (Integer) o;

            return plotSQL.hasRow("SELECT uuid FROM plot_members WHERE id=" + plotID + " AND uuid='" + uuid + "';");
        } else if (regionType == RegionType.ZONE) {

            Integer zoneID = (Integer) o;

            return plotSQL.hasRow("SELECT uuid FROM zone_members WHERE id=" + zoneID + " AND uuid='" + uuid + "';");
        }

        return false;
    }

    public void invite(NetworkUser u, String uuid) {

        // Check whether the player is not already invites to the region, plot or zone, if true skip them.
        if (regionType == RegionType.REGION) {

            Region region = (Region) o;

            // Send invite via chat.
            // The invite will be active until they disconnect from the network.
            // They will need to run a command to actually join the plot.
            regionSQL.update("INSERT INTO region_invites(region,owner,uuid) VALUES('" + region.regionName() + "','" +
                    u.player.getUniqueId() + "','" + uuid + "');");

            String name = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "'");
            Network.getInstance().getOnlineUserByUuid(uuid).ifPresentOrElse(onlineUser -> {
                        EventManager.createEvent(uuid, "network", onlineUser.getServer(),
                                "invite region " + region.regionName());
                        u.player.sendMessage(ChatUtils.success("Invited %s to region %s", name,
                                region.getTag(u.player.getUniqueId().toString())));
                    },
                    () -> u.player.sendMessage(ChatUtils.error("%s is no longer online.", name)));
        } else if (regionType == RegionType.PLOT) {

            Integer plotID = (Integer) o;

            // Send invite via chat.
            // The invite will be active until they disconnect from the network.
            // They will need to run a command to actually join the plot.
            plotSQL.update("INSERT INTO plot_invites(id,owner,uuid) VALUES(" + plotID + ",'" +
                    u.player.getUniqueId() + "','" + uuid + "');");

            String name = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "'");
            Network.getInstance().getOnlineUserByUuid(uuid).ifPresentOrElse(onlineUser -> {
                        EventManager.createEvent(uuid, "network", onlineUser.getServer(),
                                "invite plot " + plotID);
                        u.player.sendMessage(ChatUtils.success("Invited %s to your Plot.", name));
                    },
                    () -> u.player.sendMessage(ChatUtils.error("%s is no longer online.", name)));
        } else if (regionType == RegionType.ZONE) {

            Integer zoneID = (Integer) o;

            // Send invite via chat.
            // The invite will be active until they disconnect from the network.
            // They will need to run a command to actually join the plot.
            plotSQL.update("INSERT INTO zone_invites(id,owner,uuid) VALUES(" + zoneID + ",'" +
                    u.player.getUniqueId() + "','" + uuid + "');");

            String name = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "'");
            Network.getInstance().getOnlineUserByUuid(uuid).ifPresentOrElse(onlineUser -> {
                        EventManager.createEvent(uuid, "network", onlineUser.getServer(),
                                "invite zone " + zoneID);
                        u.player.sendMessage(ChatUtils.success("Invited %s to zone your Zone.", name));
                    },
                    () -> u.player.sendMessage(ChatUtils.error("%s is no longer online.", name)));
        }
    }
}