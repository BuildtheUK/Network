package net.bteuk.network.gui.regions;

import net.bteuk.network.core.Time;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.util.ArrayList;

public class RegionMembers extends NetworkRefreshableGui {

    private final Region region;
    private final GlobalSQL globalSQL;
    private final RegionManager regionManager;
    private int page;
    private boolean transfer;

    public RegionMembers(GuiProvider provider, Region region) {

        super(provider, 45, Component.text("Region Members", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.region = region;

        page = 1;

        transfer = false;

        this.globalSQL = provider.globalSQL();
        this.regionManager = provider.regionManager();
    }

    protected void createGui() {

        // Get all members of the region.
        ArrayList<String> region_members = regionManager.getMembers(region);

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // Switch from kick member mode to transfer owner mode.
        if (transfer) {

            setItem(8, Utils.createItem(Material.MAGENTA_GLAZED_TERRACOTTA, 1, Utils.title("Switch Mode"), Utils.line("Converts gui to kick members."),
                            Utils.line("Clicking on a player head"), Utils.line("will kick them from the region.")),

                    (NetworkUser u) -> {

                        transfer = !transfer;
                        this.refresh();
                    });
        } else {

            setItem(8, Utils.createItem(Material.MAGENTA_GLAZED_TERRACOTTA, 1, Utils.title("Switch Mode"), Utils.line("Converts gui to transfer ownership."),
                            Utils.line("Clicking on a player head will"), Utils.line("make them the owner of the region.")),

                    (NetworkUser u) -> {

                        transfer = !transfer;
                        this.refresh();
                    });
        }

        // If the page is greater than 1, add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1, Utils.title("Previous Page"), Utils.line("Open the previous page of region members.")), (NetworkUser u) ->

            {

                // Update the gui.
                page--;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // Iterate through all online players.
        for (String uuid : region_members) {

            // If uuid is yours, skip.
            if (uuid.equals(regionManager.getOwner(region))) {
                continue;
            }

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1, Utils.title("Next Page"), Utils.line("Open the next page of region members.")), (NetworkUser u) ->

                {

                    // Update the gui.
                    page++;
                    this.refresh();
                    this.updatePlayerInventory(u.player);
                });

                // Stop iterating.
                break;
            }

            // If skip is greater than 0, skip this iteration.
            if (skip > 0) {
                skip--;
                continue;
            }

            // Add player to gui.
            if (transfer) {

                setItem(slot, Utils.createPlayerSkull(uuid, 1,
                        Utils.title("Make " + globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", uuid) + " the region owner."),
                        Utils.line("Most recently in this region at " + Time.getDateTime(regionManager.lastActive(region, uuid))),
                        Utils.line("You will be demoted to region member.")), (NetworkUser u) ->

                {

                    // Make the previous owner a member.
                    regionManager.makeMember(region);

                    // Give the new player ownership.
                    regionManager.makeOwner(region, uuid);

                    // Update any requests to take into account the new region owner.
                    regionManager.updateRequests(region);

                    // Send message to user.
                    u.player.sendMessage(ChatUtils.success("Transferred ownership of the region to ")
                            .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", regionManager.getOwner(region)),
                                    NamedTextColor.DARK_AQUA)));

                    // Send message to new owner.
                    DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid, "server",
                            ChatUtils.success("You are now the owner of region %s.", regionManager.getTag(region, uuid)), true);
                    provider.chatAPI().sendDirectMessage(directMessage);

                    // Return to region info.
                    this.delete();
                    u.mainGui = null;

                    u.mainGui = new RegionInfo(provider, region, u.player.getUniqueId().toString());
                    u.mainGui.open(u.player);
                });
            } else {

                setItem(slot, Utils.createPlayerSkull(uuid, 1,
                        Utils.title("Kick " + globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", uuid) + " from the region."),
                        Utils.line("Most recently in this region at: ").append(Component.text(Time.getDateTime(regionManager.lastActive(region, uuid))))), (NetworkUser u) ->

                {
                    // Remove them from the region.
                    regionManager.leaveRegion(region, uuid, ChatUtils.error("You have been kicked from region %s", regionManager.getTag(region, uuid)));

                    // Send message to user.
                    u.player.sendMessage(ChatUtils.success("Kicked %s from the region", globalSQL.getString("SELECT name FROM player_data WHERE uuid=?;", uuid)));

                    // Refresh the gui.
                    // Delay this action so the user can be kicked, even if on another server.
                    Bukkit.getScheduler().runTaskLater(provider.instance(), this::refresh, 20L);
                });
            }

            // Increase slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, basically add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        // Return to plot info menu.
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"),
                Utils.line("Return to the menu of region ").append(Component.text(regionManager.getTag(region, regionManager.getOwner(region))))), (NetworkUser u) ->

        {

            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Switch back to plot info.
            u.mainGui = new RegionInfo(provider, region, u.player.getUniqueId().toString());
            u.mainGui.open(u.player);
        });
    }
}
