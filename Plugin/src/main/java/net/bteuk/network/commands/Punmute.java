package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.socket.MessageSender;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Command for personal muting; allows you to mute any player for the current session.
 */
public class Punmute extends PmuteAction {

    private static final Component ERROR = ChatUtils.error("/punmute [player]");

    public Punmute(Network instance, MessageSender messageSender) {
        super(instance, ERROR, messageSender);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        onCommand(stack, args, false);
    }

    @Override
    public String getLabel() {
        return "punmute";
    }

    @Override
    public String getDescription() {
        return "Unmute a player";
    }
}
