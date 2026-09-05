package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Command to remove a role from a player.
 */
public class Demote extends PromotionAction {
    private static final Component ERROR = ChatUtils.error("/demote [player] [role]");

    public Demote(Network instance, Roles roles, CustomChat chat) {
        super(instance, roles, chat, ERROR);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        CommandSender sender = stack.getSender();
        if (hasPermission(sender, "uknet.staff.demote")) {
            onCommand(sender, args, true);
        }
    }

    @Override
    public String getLabel() {
        return "demote";
    }

    @Override
    public String getDescription() {
        return "Remove a role from a player.";
    }
}