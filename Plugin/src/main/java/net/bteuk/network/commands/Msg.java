package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.commands.tabcompleters.PlayerSelector;
import net.bteuk.network.lib.dto.PrivateMessage;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.socket.MessageSender;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * General message command, includes /tell, /w and /msg
 */
public class Msg extends AbstractCommand {

    private static final Component ERROR = ChatUtils.error("/msg [player] <message>");

    private final String commandName;
    private final MessageSender messageSender;

    public Msg(Network instance, String commandName, MessageSender messageSender) {
        this.commandName = commandName;
        this.messageSender = messageSender;
        setTabCompleter(new PlayerSelector(instance));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Get the uuid of the player.
        if (args.length < 2) {
            player.sendMessage(ERROR);
            return;
        }

        // Send a direct message, the message is created using all other command arguments.
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        PrivateMessage privateMessage = new PrivateMessage(ChatChannels.GLOBAL.getChannelName(), player.getName(), args[0],message, false);
        messageSender.sendSocketMessage(privateMessage);
    }

    public static Msg of(Network instance, String label, MessageSender messageSender) {
        return new Msg(instance, label, messageSender);
    }

    @Override
    public String getLabel() {
        return commandName;
    }

    @Override
    public String getDescription() {
        return "Sends a direct message to a player.";
    }
}
