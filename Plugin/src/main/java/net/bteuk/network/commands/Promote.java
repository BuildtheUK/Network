package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Command to add a role to a player.
 */
public class Promote extends PromotionAction {
    private static final Component ERROR = ChatUtils.error("/promote [player] [role]");

    public Promote(Network instance, Roles roles, CustomChat chat) {
        super(instance, roles, chat, ERROR);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        CommandSender sender = stack.getSender();
        if (hasPermission(sender, "uknet.staff.promote")) {
            onCommand(sender, args, false);
        }
    }

    @Override
    public String getLabel() {
        return "promote";
    }

    @Override
    public String getDescription() {
        return "Add a role to a player.";
    }
}