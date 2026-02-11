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

public class Homes extends AbstractCommand {

    private final SQLAPI globalSQL;

    public Homes(Network instance) {
        this.globalSQL = instance.getGlobalSQL();
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check if the player has permission.
        if (!hasPermission(player, "uknet.navigation.homes")) {
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

        // Get all homes in alphabetical order.
        List<String> homes = globalSQL.getStringList("SELECT name FROM home WHERE " + "uuid='" + player.getUniqueId() + "' ORDER BY name ASC;");

        // If there are no locations notify the user.
        if (homes.isEmpty()) {
            player.sendMessage(ChatUtils.error("You don't have any homes set."));
            return;
        }

        int pages = (((homes.size() - 1) / 16) + 1);

        // Get the first 16 homes and add them to a string.
        // If the page is greater than 1 and there are enough homes to support that, then post that page.

        if (((page - 1) * 16) >= homes.size()) {

            if (homes.size() <= 16) {
                player.sendMessage(ChatUtils.error("There is only ").append(Component.text("1", NamedTextColor.DARK_RED)).append(ChatUtils.error(" page of homes.")));
            } else {
                player.sendMessage(ChatUtils.error("There are only ").append(Component.text(pages, NamedTextColor.DARK_RED)).append(ChatUtils.error(" pages of homes.")));
            }
            return;
        }

        // Show this page of homes.
        Component message = Component.text("");

        // If this isn't the first page show command for previous page.
        if (page > 1) {

            // Create previousPage button with hover and click event.
            Component previousPage = Component.text("⏪⏪⏪", TextColor.color(212, 113, 15));
            previousPage = previousPage.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("Click to view the previous page of homes.")));
            previousPage = previousPage.clickEvent(ClickEvent.runCommand("/homes " + (page - 1)));

            // Add previousPage button at the start of the first line.
            message = message.append(previousPage);
            message = message.append(Component.text(" "));
        }

        message = message.append(
                Component.text("Page ", NamedTextColor.GREEN).append(Component.text(page, TextColor.color(245, 221, 100))).append(Component.text("/", NamedTextColor.GREEN))
                        .append(Component.text(pages, TextColor.color(245, 221, 100))));

        // If this isn't the last page show command for the next page.
        if ((page * 16) < homes.size()) {

            // Create the previousPage button with hover and click-event.
            Component nextPage = Component.text("⏩⏩⏩\n", TextColor.color(212, 113, 15));
            nextPage = nextPage.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("Click to " + "view the next page of homes.")));
            nextPage = nextPage.clickEvent(ClickEvent.runCommand("/homes " + (page + 1)));

            // Add the previousPage button at the start of the first line.
            message = message.append(Component.text(" "));
            message = message.append(nextPage);
        } else {

            message = message.append(Component.text("\n"));
        }

        // Number of entries to skip.
        int skip = (page - 1) * 16;

        for (String home : homes) {
            if (skip > 0) {
                skip--;
                continue;
            }

            // If it isn't the last entry, as a comma at the end.
            // This is calculated by it either being the 16th entry, or the last in the list.
            if (((homes.indexOf(home) + 1) % 16) == 0 || (homes.indexOf(home) + 1 == homes.size())) {

                message = message.append(createHome(home));
                break;
            } else {

                message = message.append(createHome(home)).append(Component.text(", ", NamedTextColor.WHITE));
            }
        }

        player.sendMessage(message);
    }

    private Component createHome(String name) {

        Component home;
        if (name == null) {
            home = Component.text("<default>", TextColor.color(245, 221, 100));
            home = home.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to " + "teleport to your default home.")));
            home = home.clickEvent(ClickEvent.runCommand("/home"));
        } else {
            home = Component.text(name, TextColor.color(245, 221, 100));
            home = home.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Component.text("Click to " + "teleport to " + name)));
            home = home.clickEvent(ClickEvent.runCommand("/home " + name));
        }

        return home;
    }

    @Override
    public String getLabel() {
        return "homes";
    }

    @Override
    public String getDescription() {
        return "Like warps, but for homes, shows all homes the player has set.";
    }
}
