package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.CustomChat;
import net.bteuk.network.lib.dto.ReplyMessage;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class Reply extends AbstractCommand {
    private static final String ERROR = "/r [message]";
    private final CustomChat chat;

    public Reply(CustomChat chat) {
        this.chat = chat;
    }

    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Get the uuid of the player.
        if (args.length < 1) {
            player.sendMessage(ChatUtils.error(ERROR));
            return;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
        ReplyMessage replymessage = new ReplyMessage(ChatChannels.GLOBAL.getChannelName(),player.getName(),message,false);
        chat.sendSocketMessage(replymessage);
    }

    @Override
    public String getLabel() {
        return "reply";
    }

    @Override
    public String getDescription() {
        return "sends a direct message to the last player you messaged";
    }

    @Override
    public List<String> getAliases() {
        return List.of("r");
    }
}
