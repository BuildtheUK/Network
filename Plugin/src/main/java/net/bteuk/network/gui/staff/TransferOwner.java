package net.bteuk.network.gui.staff;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;

public class TransferOwner extends NetworkRefreshableGui {

    private final Region region;
    private final GlobalSQL globalSQL;
    private final RegionManager regionManager;
    private int page;

    public TransferOwner(GuiProvider provider, Region region) {

        super(provider, 45, Component.text("Transfer Ownership", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.region = region;

        page = 1;

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

        // If page is greater than 1 add a previous page button.
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
            setItem(slot,
                    Utils.createPlayerSkull(uuid, 1, Utils.title("Make " + globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "';") + " the region owner."),
                            Utils.line("The previous owner will be demoted to a member.")), (NetworkUser u) ->

                    {
                        // Make the previous owner a member.
                        regionManager.makeMember(region);

                        // Give the new player ownership.
                        regionManager.makeOwner(region, uuid);

                        // Update any requests to take into account the new region owner.
                        regionManager.updateRequests(region);

                        // Send a message to user.
                        u.player.sendMessage(ChatUtils.success("Transferred ownership of the region to ")
                                .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid " + "='" + regionManager.getOwner(region) + "';"),
                                        NamedTextColor.DARK_AQUA)));

                        // Refresh the gui.
                        this.refresh();
                    });

            // Increase the slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, basically add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        // Return to the manage region menu.
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"),
                Utils.line("Return to manage region ").append(Component.text(region.regionName(), NamedTextColor.GRAY))), (NetworkUser u) -> {

            // Delete this gui.
            this.delete();

            // Switch back to plot info.
            u.staffGui = new ManageRegion(provider, u, region);
            u.staffGui.open(u.player);
        });
    }
}
