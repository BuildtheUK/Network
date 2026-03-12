package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.socket.MessageSender;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Command for personal muting, allows you to mute any player for the current session.
 */
public class Pmute extends PmuteAction {

    private static final Component ERROR = ChatUtils.error("/pmute [player]");

    public Pmute(Network instance, MessageSender messageSender) {
        super(instance, ERROR, messageSender);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        onCommand(stack, args, true);
    }

    @Override
    public String getLabel() {
        return "pmute";
    }

    @Override
    public String getDescription() {
        return "Mute a player";
    }
}
