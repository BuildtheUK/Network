package net.bteuk.network.gui.staff;

import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;

public class KickMembers extends Gui {

    private final Region region;
    private final GlobalSQL globalSQL;
    private int page;

    public KickMembers(Region region) {

        super(45, Component.text("Kick Member", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.region = region;

        page = 1;

        globalSQL = Network.getInstance().getGlobalSQL();

        createGui();
    }

    private void createGui() {

        // Get all members of the region.
        ArrayList<String> region_members = region.getMembers();

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // If page is greater than 1 add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1,
                            Utils.title("Previous Page"),
                            Utils.line("Open the previous page of region members.")),
                    (NetworkUser u) ->

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

                setItem(26, Utils.createItem(Material.ARROW, 1,
                                Utils.title("Next Page"),
                                Utils.line("Open the next page of region members.")),
                        (NetworkUser u) ->

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

            // If skip is greater than 0, skip this iteration.
            if (skip > 0) {
                skip--;
                continue;
            }

            // Add player to gui.
            setItem(slot, Utils.createPlayerSkull(uuid, 1,
                            Utils.title("Kick " + globalSQL.getString(
                                    "SELECT name FROM player_data WHERE uuid='" + uuid + "';") + " from the region.")),
                    (NetworkUser u) ->

                    {
                        // Remove them from the region.
                        region.leaveRegion(uuid, ChatUtils.error("You have been kicked from region %s",
                                region.getTag(uuid)));

                        // Send message to user.
                        u.player.sendMessage(ChatUtils.success("Kicked ")
                                .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid " +
                                        "='" + uuid + "';"), NamedTextColor.DARK_AQUA))
                                .append(ChatUtils.success(" from the region")));

                        // Refresh the gui.
                        this.refresh();
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

        // Return to plot info menu.
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Return to manage region ")
                                .append(Component.text(region.regionName(), NamedTextColor.GRAY))),
                (NetworkUser u) ->

                {

                    // Delete this gui.
                    this.delete();
                    u.staffGui = null;

                    // Switch back to plot info.
                    u.staffGui = new ManageRegion(u, region);
                    u.staffGui.open(u.player);
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }
}
