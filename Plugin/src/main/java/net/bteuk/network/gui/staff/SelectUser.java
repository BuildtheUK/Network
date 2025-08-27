package net.bteuk.network.gui.staff;

import net.bteuk.network.Network;
import net.bteuk.network.lib.dto.OnlineUser;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.Time;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.ModerationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

import static net.bteuk.network.utils.enums.ModerationType.UNBAN;
import static net.bteuk.network.utils.enums.ModerationType.UNMUTE;

public class SelectUser extends Gui {

    List<String> users;

    ModerationType type;

    int page;

    GlobalSQL globalSQL;

    public SelectUser(ModerationType type) {

        super(45, Component.text("Select User for " + type.label, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.type = type;

        globalSQL = Network.getInstance().getGlobalSQL();

        // Select all the players to show in the menu depending on the ModerationType.
        switch (type) {

            case BAN, MUTE, KICK ->

                // Get online users.
                    users = Network.getInstance().getOnlineUsers().stream().map(OnlineUser::getUuid).toList();

            case UNBAN ->

                // Get banned users.
                    users = globalSQL.getStringList(
                            "SELECT uuid FROM moderation WHERE end_time>" + Time.currentTime() + " AND type='ban'");

            case UNMUTE ->

                // Get muted users.
                    users = globalSQL.getStringList(
                            "SELECT uuid FROM moderation WHERE end_time>" + Time.currentTime() + " AND type='mute'");
        }

        createGui();
    }

    private void createGui() {

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // If page is greater than 1 add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1,
                            Utils.title("Previous Page"),
                            Utils.line("Open the previous page of regions.")),
                    u ->

                    {

                        // Update the gui.
                        page--;
                        this.refresh();
                        u.player.getOpenInventory().getTopInventory().setContents(this.getInventory().getContents());
                    });
        }

        // Make a button for each user.
        for (String uuid : users) {

            // If skip is greater than 0, skip this iteration.
            if (skip > 0) {
                skip--;
                continue;
            }

            // If the slot is greater than the number that fit in a page, create a new page.
            if (slot > 34) {

                setItem(26, Utils.createItem(Material.ARROW, 1,
                                Utils.title("Next Page"),
                                Utils.line("Open the next page of users.")),
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

            // Create a menu for the moderation action for this specific player.
            // Ban and muting has a submenu to select duration and reason.
            // Kicking just prompts staff to type the reason in chat.
            // Unban and unmute is just a simple click.
            String name = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid + "';");
            String player_skin = globalSQL.getString("SELECT player_skin FROM player_data WHERE uuid='" + uuid + "';");

            switch (type) {

                case BAN, MUTE, KICK -> // Ban/mute/kick the player.
                        setItem(slot, Utils.createCustomSkullWithFallback(player_skin, Material.RED_CONCRETE, 1,
                                        Utils.title(type.label + " " + name),
                                        Utils.line("Opens the " + type.label.toLowerCase(Locale.ROOT) + " menu to set" +
                                                " the parameters.")),
                                u ->

                                {

                                    // Open the kick menu.
                                    this.delete();
                                    u.staffGui = new ModerationActionGui(type, uuid);
                                    u.staffGui.open(u.player);
                                });

                case UNBAN, UNMUTE -> // Unban/unmute the player.
                        setItem(slot, Utils.createCustomSkullWithFallback(player_skin, Material.LIME_CONCRETE, 1,
                                        Utils.title(type.label + " " + name),
                                        Utils.line(type.label + " the player immediately.")),

                                (NetworkUser u) -> {

                                    u.player.closeInventory();

                                    if (type == UNBAN) {

                                        // Unban the player.
                                        u.player.sendMessage(Network.getInstance().getUnban().unbanPlayer(name, uuid));
                                    } else if (type == UNMUTE) {

                                        // Unmute the player.
                                        u.player.sendMessage(Network.getInstance().getUnmute().unmutePlayer(name,
                                                uuid));
                                    }

                                    // Delete the gui and remove it from the user.
                                    this.delete();
                                    u.staffGui = null;
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

        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Previous Page"),
                        Utils.line("Open the moderation menu.")),
                u ->

                {

                    // Return to request menu.
                    this.delete();
                    u.staffGui = null;

                    u.staffGui = new ModerationGui();
                    u.staffGui.open(u.player);
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }
}
