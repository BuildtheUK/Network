package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.exceptions.InvalidFormatException;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.enums.TimesOfDay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static java.lang.Math.floorMod;

public class Ptime extends AbstractCommand {

    private static final Component RESET_PLAYER_TIME = ChatUtils.success("Reset player time, using server time.");
    private static final Component SET_PLAYER_TIME = ChatUtils.success("Set player time to ");
    private static final Component INVALID_FORMAT = ChatUtils.error("Invalid time format, try using HH:mm or " +
            "Minecraft ticks.");

    public Ptime() {
        setTabCompleter(new FixedArgSelector(Arrays.stream(TimesOfDay.values()).map(t -> t.label).toList(), 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Permission check.
        if (!hasPermission(player, "uknet.ptime")) {
            return;
        }

        // No args implies setting the player time to the default (server time).
        if (args.length == 0) {
            player.resetPlayerTime();
            player.sendMessage(RESET_PLAYER_TIME);
            return;
        }

        int ticks;

        // Check arguments.
        // There are multiple accepted formats.
        // First check for written times, supported times are "sunrise", "day", "morning", "noon", "afternoon",
        // "sunset", "night", "midnight".
        try {
            if (Arrays.stream(TimesOfDay.values()).map(t -> t.label).anyMatch(t -> args[0].equalsIgnoreCase(t))) {

                ticks = TimesOfDay.valueOf(args[0].toUpperCase()).ticks;

                // Check if the argument contains AM or PM get the number before that and convert it to Minecraft time.
            } else if (args[0].toLowerCase().contains("pm")) {

                String[] firstArg = args[0].toLowerCase().split("pm");

                // Try parsing the first arg as a number.
                int time = Integer.parseInt(firstArg[0]);

                if (time > 0 && time <= 12) {
                    ticks = (12000 + convertHourToTicks(time % 12)) % 24000;
                } else {
                    throw new InvalidFormatException("Invalid time format");
                }
            } else if (args[0].toLowerCase().contains("am")) {

                String[] firstArg = args[0].toLowerCase().split("am");

                // Try parsing the first arg as a number.
                int time = Integer.parseInt(firstArg[0]);

                if (time > 0 && time <= 12) {
                    ticks = convertHourToTicks(time % 12);
                } else {
                    throw new InvalidFormatException("Invalid time format");
                }
                // If the argument contains : use a 24-hour clock and include minutes.
            } else if (args[0].contains(":")) {

                String[] firstArg = args[0].split(":");

                int hours = Integer.parseInt(firstArg[0]);
                int minutes = Integer.parseInt(firstArg[1]);

                if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                    throw new InvalidFormatException("Invalid time format");
                }

                ticks = convertHourToTicks(hours) + convertMinutesToTicks(minutes);
            } else {
                // If the arguments is between 3-5 numbers then use Minecraft time directly.
                int minecraftTicks = Integer.parseInt(args[0]);

                if (minecraftTicks > 0) {
                    ticks = minecraftTicks % 24000;
                } else {
                    throw new InvalidFormatException("Invalid time format");
                }
            }
        } catch (NumberFormatException | InvalidFormatException ex) {
            player.sendMessage(INVALID_FORMAT);
            return;
        }

        // Set time and send feedback.
        player.setPlayerTime(ticks, false);
        player.sendMessage(SET_PLAYER_TIME.append(Component.text(args[0] + " (" + ticks + ")",
                NamedTextColor.DARK_AQUA)));
    }

    private int convertHourToTicks(int hour) {
        hour -= 6;
        hour = floorMod(hour, 24);
        return hour * 1000;
    }

    private int convertMinutesToTicks(int minutes) {
        return minutes * 1000 / 60;
    }

    @Override
    public String getLabel() {
        return "ptime";
    }

    @Override
    public String getDescription() {
        return "Sets the time of day for the player";
    }
}
