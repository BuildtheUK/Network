package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Pweather extends AbstractCommand {

    private static final Component HELP_MESSAGE = Utils.error("/pweather <clear|downfall>");

    public Pweather() {
        setTabCompleter(new FixedArgSelector(
                Arrays.stream(WeatherType.values()).map(value -> value.toString().toLowerCase()).toList(), 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Permission check.
        if (!hasPermission(player, "uknet.pweather")) {
            return;
        }

        WeatherType weatherType = WeatherType.CLEAR;
        if (args.length > 0) {
            try {
                weatherType = WeatherType.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ignored) {
                player.sendMessage(HELP_MESSAGE);
                return;
            }
        }

        player.setPlayerWeather(weatherType);
        player.sendMessage(Utils.success("Set weather to %s", weatherType.name()));
    }

    @Override
    public String getLabel() {
        return "pweather";
    }

    @Override
    public String getDescription() {
        return "Sets the weather for the player";
    }
}
