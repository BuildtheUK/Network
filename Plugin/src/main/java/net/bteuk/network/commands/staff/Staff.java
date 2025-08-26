package net.bteuk.network.commands.staff;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.api.ChatAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.core.Constants;
import net.bteuk.network.gui.staff.StaffGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;
import static net.bteuk.network.lib.enums.ChatChannels.STAFF;

@Log
public class Staff extends AbstractCommand {

    private final Network instance;
    private final Constants constants;
    private final GlobalSQL globalSQL;
    private final ChatAPI chatAPI;

    public Staff(Network instance, Constants constants, GlobalSQL globalSQL, ChatAPI chatAPI) {
        this.instance = instance;
        this.constants = constants;
        this.globalSQL = globalSQL;
        this.chatAPI = chatAPI;
    }

    public void openStaffMenu(NetworkUser u) {

        // Check if the gui exists.
        // If it does refresh and open it.
        // If no gui exists open the staff menu.

        if (u.staffGui != null) {

            u.staffGui.refresh();
            u.staffGui.open(u);
        } else {

            u.staffGui = new StaffGui(u);
            u.staffGui.open(u);
        }
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player p = getPlayer(stack);
        if (p == null) {
            return;
        }

        NetworkUser u = instance.getUser(p);

        // Check if user is member of staff.
        // Architects can open the menu but not use the staff chat.
        if (!(hasPermission(p, "uknet.staff"))) {
            if (hasPermission(p, "uknet.staff.menu")) {
                openStaffMenu(u);
            }
            return;
        }

        // If u is null, cancel.
        if (u == null) {
            log.severe("User " + p.getName() + " can not be found!");
            p.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        // If the first arg is chat, switch the player to and from staff chat if enabled.
        if (!constants.standalone()) {
            if (args.length > 0 && constants.staffChat()) {
                if (args[0].equalsIgnoreCase("chat")) {
                    String channel = GLOBAL.getChannelName();
                    if (u.getChatChannel().equals(STAFF.getChannelName())) {
                        u.player.sendMessage(ChatUtils.success("Disabled staff chat."));
                    } else {
                        // Set the chat channel to staff.
                        channel = STAFF.getChannelName();
                        u.player.sendMessage(ChatUtils.success("Enabled staff chat."));
                    }
                    // Set channel.
                    u.setChatChannel(channel);
                    globalSQL.update("UPDATE player_data SET chat_channel='" + channel + "' " + "WHERE uuid='" + p.getUniqueId() + "';");
                } else {
                    // Send a message in staff-chat, by temporarily setting the player's channel to staff.
                    u.setChatChannel(STAFF.getChannelName());
                    chatAPI.sendChatMessage(CustomChat.getChatMessage(Component.text(String.join(" ", args)), u));
                    u.setChatChannel(GLOBAL.getChannelName());
                }
                return;
            }
        } else {
            u.player.sendMessage(ChatUtils.error("Staff chat is currently not available in standalone mode!"));
        }

        // If the player has a previous gui, open that.
        openStaffMenu(u);
    }

    @Override
    public String getLabel() {
        return "staff";
    }

    @Override
    public String getDescription() {
        return "Opens the Staff Menu.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("st");
    }
}
