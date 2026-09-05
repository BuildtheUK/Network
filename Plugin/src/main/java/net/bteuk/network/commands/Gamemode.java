package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.StringUtils;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class Gamemode extends AbstractCommand {

    // Gamemodes.
    private final ArrayList<String> gamemodes = new ArrayList<>(Arrays.asList("creative", "spectator", "adventure", "survival"));

    private final Constants constants;

    // Constructor to enable the command.
    public Gamemode(Constants constants) {
        this.constants = constants;
        setTabCompleter(new FixedArgSelector(gamemodes, 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check allowed server type.
        if (constants.serverType() != ServerType.PLOT && constants.serverType() != ServerType.EARTH) {
            player.sendMessage(ChatUtils.error("You do not have permission to use this command here."));
            return;
        }

        // Check if the player has permission.
        if (!hasPermission(player, "uknet.gamemode")) {
            return;
        }

        // If no args are given, send a list of clickable gamemodes.
        // Else switch to the given gamemode in arg[0], if it exists.
        if (args.length == 0) {

            Component message = Component.text("Available gamemodes: ", NamedTextColor.GREEN);

            for (int i = 0; i < gamemodes.size(); i++) {

                Component gamemode;

                // If the player is in the gamemode, highlight it.
                if (player.getGameMode() == GameMode.valueOf(gamemodes.get(i).toUpperCase(Locale.ROOT))) {
                    gamemode = Component.text(StringUtils.capitalize(gamemodes.get(i)), TextColor.color(245, 173, 100));
                    gamemode = gamemode.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("You" + " are already in this gamemode.")));
                } else {
                    gamemode = Component.text(StringUtils.capitalize(gamemodes.get(i)), TextColor.color(245, 221, 100));
                    gamemode = gamemode.clickEvent(ClickEvent.runCommand("/gamemode " + gamemodes.get(i)));
                    gamemode = gamemode.hoverEvent(
                            HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("Click to switch to " + StringUtils.capitalize(gamemodes.get(i)))));
                }

                message = message.append(gamemode);

                // Add comma is not the list value.
                if (i < (gamemodes.size() - 1)) {
                    message = message.append(Utils.line(", "));
                }
            }

            player.sendMessage(message);
        } else {

            // Check if the gamemode exists.
            if (!gamemodes.contains(args[0].toLowerCase(Locale.ROOT))) {

                Component error = Component.text(args[0], NamedTextColor.DARK_RED);
                error = error.append(ChatUtils.error(" is not a valid gamemode"));

                player.sendMessage(error);
                return;
            }

            // Set the player to this gamemode.
            player.setGameMode(GameMode.valueOf(args[0].toUpperCase(Locale.ROOT)));
            player.sendMessage(ChatUtils.success("Set gamemode to ").append(Component.text(StringUtils.capitalize(args[0].toLowerCase(Locale.ROOT)), NamedTextColor.DARK_AQUA)));
        }
    }

    @Override
    public String getLabel() {
        return "gamemode";
    }

    @Override
    public String getDescription() {
        return "Switch gamemode.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("gm");
    }
}
