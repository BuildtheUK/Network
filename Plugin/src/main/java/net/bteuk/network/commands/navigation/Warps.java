package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Warps extends AbstractCommand {

    private final SQLAPI globalSQL;

    public Warps(Network instance) {
        this.globalSQL = instance.getGlobalSQL();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Set default page to 1.
        int page = 1;

        // Check if a page is specified.
        if (args.length > 0) {
            try {

                page = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                // Ignore the arguments altogether.
            }
        }

        // Get all locations in alphabetical order.
        List<String> locations = globalSQL.getStringList("SELECT location FROM location_data ORDER BY location ASC;");

        // If there are no locations, notify the user.
        if (locations.isEmpty()) {
            player.sendMessage(ChatUtils.error("There are currently no warps available."));
            return;
        }

        int pages = (((locations.size() - 1) / 16) + 1);

        // Get the first 16 locations and add them to a string.
        // If the page is greater than 1 and there are enough locations to support that, then post that page.

        if (((page - 1) * 16) >= locations.size()) {

            if (locations.size() <= 16) {
                player.sendMessage(ChatUtils.error("There is only ")
                        .append(Component.text("1", NamedTextColor.DARK_RED))
                        .append(ChatUtils.error(" page of warps.")));
            } else {
                player.sendMessage(ChatUtils.error("There are only ")
                        .append(Component.text(pages, NamedTextColor.DARK_RED))
                        .append(ChatUtils.error(" pages of warps.")));
            }

            return;
        }

        // Show this page of warps.
        Component message = Component.text("");

        // If this isn't the first page show command for previous page.
        if (page > 1) {

            // Create previousPage button with hover and click event.
            Component previousPage = Component.text("⏪⏪⏪", TextColor.color(212, 113, 15));
            previousPage = previousPage.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line(
                    "Click to view the previous page of warps.")));
            previousPage = previousPage.clickEvent(ClickEvent.runCommand("/warps " + (page - 1)));

            // Add previousPage button at the start of the first line.
            message = message.append(previousPage);
            message = message.append(Component.text(" "));
        }

        message = message.append(Component.text("Page ", NamedTextColor.GREEN)
                .append(Component.text(page, TextColor.color(245, 221, 100)))
                .append(Component.text("/", NamedTextColor.GREEN))
                .append(Component.text(pages, TextColor.color(245, 221, 100))));

        // If this isn't the last page show command for the next page.
        if ((page * 16) < locations.size()) {

            // Create previousPage button with hover and click event.
            Component nextPage = Component.text("⏩⏩⏩\n", TextColor.color(212, 113, 15));
            nextPage = nextPage.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("Click to " +
                    "view the next page of warps.")));
            nextPage = nextPage.clickEvent(ClickEvent.runCommand("/warps " + (page + 1)));

            // Add previousPage button at the start of the first line.
            message = message.append(Component.text(" "));
            message = message.append(nextPage);
        } else {

            message = message.append(Component.text("\n"));
        }

        // Number of entries to skip.
        int skip = (page - 1) * 16;

        for (String location : locations) {
            if (skip > 0) {
                skip--;
                continue;
            }

            // If it isn't the last entry, as a comma at the end.
            // This is calculated by it either being the 16th entry or the last in the list.
            if (((locations.indexOf(location) + 1) % 16) == 0 || (locations.indexOf(
                    location) + 1 == locations.size())) {

                message = message.append(createWarp(location));
                break;
            } else {

                message = message.append(createWarp(location)).append(Component.text(", ", NamedTextColor.WHITE));
            }
        }

        player.sendMessage(message);
    }

    private Component createWarp(String name) {

        Component warp = Component.text(name, TextColor.color(245, 221, 100));
        warp = warp.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to teleport " +
                "to " + name)));
        warp = warp.clickEvent(ClickEvent.runCommand("/warp " + name));

        return warp;
    }

    @Override
    public String getLabel() {
        return "warps";
    }

    @Override
    public String getDescription() {
        return "List all warps on the server, 16 per page.";
    }
}
