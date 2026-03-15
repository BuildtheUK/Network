package net.bteuk.network.gui.staff;

import net.bteuk.network.core.Time;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.dto.OnlineUser;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.ModerationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

import static net.bteuk.network.utils.enums.ModerationType.UNBAN;

public class SelectUser extends NetworkRefreshableGui {

    private List<String> users;

    private final ModerationType type;

    private int page;

    private final GlobalSQL globalSQL;

    public SelectUser(GuiProvider provider, ModerationType type) {

        super(provider, 45, Component.text("Select User for " + type.label, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.type = type;
        this.globalSQL = provider.globalSQL();

        // Select all the players to show in the menu depending on the ModerationType.
        switch (type) {

            case BAN, MUTE, KICK ->
                // Get online users.
                    users = provider.instance().getOnlineUsers().stream().map(OnlineUser::getUuid).toList();
            case UNBAN ->
                // Get banned users.
                    users = globalSQL.getStringList("SELECT uuid FROM moderation WHERE end_time>" + Time.currentTime() + " AND type='ban'");
            case UNMUTE ->
                // Get muted users.
                    users = globalSQL.getStringList("SELECT uuid FROM moderation WHERE end_time>" + Time.currentTime() + " AND type='mute'");
        }
    }

    protected void createGui() {

        // Slot count.
        int slot = 10;

        // Skip count.
        int skip = 21 * (page - 1);

        // If the page is greater than 1, add a previous page button.
        if (page > 1) {
            setItem(18, Utils.createItem(Material.ARROW, 1, Utils.title("Previous Page"), Utils.line("Open the previous page of regions.")), (NetworkUser u) -> {

                // Update the gui.
                page--;
                this.refresh();
                this.updatePlayerInventory(u.player);
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

                setItem(26, Utils.createItem(Material.ARROW, 1, Utils.title("Next Page"), Utils.line("Open the next page of users.")), (NetworkUser u) -> {

                    // Update the gui.
                    page++;
                    this.refresh();
                    this.updatePlayerInventory(u.player);
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
                        setItem(slot, Utils.createCustomSkullWithFallback(provider.instance(), player_skin, Material.RED_CONCRETE, 1, Utils.title(type.label + " " + name),
                                Utils.line("Opens the " + type.label.toLowerCase(Locale.ROOT) + " menu to set" + " the parameters.")), (NetworkUser u) ->

                        {

                            // Open the kick menu.
                            this.delete();
                            u.staffGui = new ModerationActionGui(provider, type, uuid);
                            u.staffGui.open(u.player);
                        });

                case UNBAN, UNMUTE -> // Unban/unmute the player.
                        setItem(slot, Utils.createCustomSkullWithFallback(provider.instance(), player_skin, Material.LIME_CONCRETE, 1, Utils.title(type.label + " " + name),
                                        Utils.line(type.label + " the player immediately.")),

                                (NetworkUser u) -> {

                                    u.player.closeInventory();

                                    if (type == UNBAN) {
                                        // Unban the player.
                                        u.player.sendMessage(provider.moderation().unbanPlayer(name, uuid));
                                    } else {
                                        // Unmute the player.
                                        u.player.sendMessage(provider.moderation().unmutePlayer(name, uuid));
                                    }

                                    // Delete the gui and remove it from the user.
                                    this.delete();
                                    u.staffGui = null;
                                });
            }

            // Increase the slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, basically add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Previous Page"), Utils.line("Open the moderation menu.")), (NetworkUser u) -> {

            // Return to the moderation menu.
            this.delete();
            u.staffGui = null;

            u.staffGui = new ModerationGui(provider);
            u.staffGui.open(u.player);
        });
    }
}
