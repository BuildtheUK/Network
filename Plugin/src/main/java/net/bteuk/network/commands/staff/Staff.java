package net.bteuk.network.commands.staff;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.staff.StaffGui;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static org.btuk.network.lib.enums.ChatChannels.GLOBAL;
import static org.btuk.network.lib.enums.ChatChannels.STAFF;

@Log
public class Staff extends AbstractCommand {

    private final GuiProvider guiProvider;

    public Staff(GuiProvider guiProvider) {
        this.guiProvider = guiProvider;
    }

    public void openStaffMenu(NetworkUser u) {

        // Check if the gui exists.
        // If it does refresh and open it.
        // If no gui exists open the staff menu.

        if (u.staffGui != null) {
            u.staffGui.open(u.player);
        } else {
            u.staffGui = new StaffGui(guiProvider, u);
            u.staffGui.open(u.player);
        }
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player p = getPlayer(stack);
        if (p == null) {
            return;
        }

        NetworkUser u = guiProvider.instance().getUser(p);

        // If u is null, cancel.
        if (u == null) {
            log.severe("User " + p.getName() + " can not be found!");
            p.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        // Check if user is member of staff.
        // Architects can open the menu but not use the staff chat.
        if (!(hasPermission(p, "uknet.staff"))) {
            // If not staff, but architect, then open menu
            if (hasPermission(p, "uknet.staff.menu")) {
                openStaffMenu(u);
            }
            // If not staff and not architect, then return
            return;
        }

        if ((args.length > 0 && guiProvider.constants().staffChat())) {
            // If the first arg is chat, switch the player to and from staff chat if enabled.
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
                guiProvider.globalSQL().update("UPDATE player_data SET chat_channel='" + channel + "' " + "WHERE uuid='" + p.getUniqueId() + "';");
            }

            else {
                // Send a message in staff-chat, by temporarily setting the player's channel to staff.
                u.setChatChannel(STAFF.getChannelName());
                guiProvider.chatAPI().sendChatMessage(CustomChat.getChatMessage(Component.text(String.join(" ", args)), u));
                u.setChatChannel(GLOBAL.getChannelName());
            }
        }
        // Else, if not staff chat, then open menu
        else
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
