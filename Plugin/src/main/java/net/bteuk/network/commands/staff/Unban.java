package net.bteuk.network.commands.staff;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.staff.Moderation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Unban extends AbstractCommand {

    private final Moderation moderation;
    private final SQLAPI globalSQL;

    public Unban(Network instance, Moderation moderation) {
        this.moderation = moderation;
        this.globalSQL = instance.getGlobalSQL();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if sender is player, then check permissions
        CommandSender sender = stack.getSender();
        if (sender instanceof Player) {
            if (!hasPermission(sender, "uknet.ban")) {
                return;
            }
        }

        // Check args.
        if (args.length < 1) {
            sender.sendMessage(ChatUtils.error("/unban <player>"));
            return;
        }

        // Check player.
        // If uuid exists for name.
        if (!globalSQL.hasRow("SELECT uuid FROM player_data WHERE name=?;", args[0])) {
            sender.sendMessage(Component.text(args[0], NamedTextColor.DARK_RED)
                    .append(ChatUtils.error(" is not a valid player.")));
            return;
        }

        String uuid = globalSQL.getString("SELECT uuid FROM player_data WHERE name=?;", args[0]);
        String name = globalSQL.getString("SELECT name FROM player_data WHERE name=?;", args[0]);

        sender.sendMessage(moderation.unbanPlayer(name, uuid));
    }

    @Override
    public String getLabel() {
        return "unban";
    }

    @Override
    public String getDescription() {
        return "Unban a previously banned player.";
    }
}
