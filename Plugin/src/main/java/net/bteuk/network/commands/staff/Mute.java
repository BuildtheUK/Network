package net.bteuk.network.commands.staff;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.core.Time;
import net.bteuk.network.exceptions.DurationFormatException;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.staff.Moderation;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Mute extends AbstractCommand {

    private final Moderation moderation;
    private final SQLAPI globalSQL;

    public Mute(Network instance, Moderation moderation) {
        this.moderation = moderation;
        this.globalSQL = instance.getGlobalSQL();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if sender is player, then check permissions
        CommandSender sender = stack.getSender();
        if (sender instanceof Player) {
            if (!hasPermission(sender, "uknet.mute")) {
                return;
            }
        }

        // Check args.
        if (args.length < 3) {
            sender.sendMessage(ChatUtils.error("/mute <player> <duration> <reason>"));
            return;
        }

        // Check player.
        // If uuid exists for name.
        if (!globalSQL.hasRow("SELECT uuid FROM player_data WHERE name='" + args[0] + "';")) {
            sender.sendMessage(Component.text(args[0], NamedTextColor.DARK_RED)
                    .append(ChatUtils.error(" is not a valid player.")));
            return;
        }

        String uuid = globalSQL.getString("SELECT uuid FROM player_data WHERE name='" + args[0] + "';");
        String name = globalSQL.getString("SELECT name FROM player_data WHERE name='" + args[0] + "';");

        // Get the duration of the ban.
        long time;
        try {

            time = moderation.getDuration(args[1]);
        } catch (DurationFormatException e) {
            sender.sendMessage(ChatUtils.error("Duration must be in ymdh format, for example 1y6m, which is 1 year " +
                    "and 6 months or 2d12h is 2 days and 12 hours."));
            return;
        }

        // Get end time of current time plus time.
        long end_time = Time.currentTime() + time;

        // Combine all remaining args to create a reason.
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        sender.sendMessage(moderation.mutePlayer(name, uuid, end_time, reason));
    }

    @Override
    public String getLabel() {
        return "mute";
    }

    @Override
    public String getDescription() {
        return "Mutes a player for a specific duration and reason.";
    }
}
