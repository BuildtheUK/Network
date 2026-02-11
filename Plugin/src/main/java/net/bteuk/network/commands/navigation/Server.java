package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.ServerSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class Server extends AbstractCommand {

    private final SQLAPI globalSQL;
    private final Constants constants;
    private final ServerAPI serverAPI;

    public Server(SQLAPI globalSQL, Constants constants, ServerAPI serverAPI) {
        this.globalSQL = globalSQL;
        this.constants = constants;
        this.serverAPI = serverAPI;
        setTabCompleter(new ServerSelector(globalSQL, constants));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check if the player has permission.
        if (!hasPermission(player, "uknet.navigation.server")) {
            return;
        }

        // If no args are given send a clickable list of servers.
        // Sort by online servers and highlight the player's current server.
        if (args.length == 0) {
            List<String> servers = globalSQL.getStringList("SELECT name FROM server_data ORDER BY online DESC");

            Component message = Component.text("Available servers: ", NamedTextColor.GREEN);

            for (int i = 0; i < servers.size(); i++) {

                // If the server is the current server highlight it.
                // If it's offline, grey it out.
                Component server;
                if (Objects.equals(constants.serverName(), servers.get(i))) {
                    server = Component.text(servers.get(i), TextColor.color(245, 173, 100));
                    server = server.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("You are" +
                            " connected to this server.")));
                } else if (globalSQL.hasRow("SELECT name FROM server_data WHERE name='" + servers.get(i) + "' AND online=0;")) {
                    server = Component.text(servers.get(i), NamedTextColor.GRAY);
                    server = server.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("This " +
                            "server is offline.")));
                } else {
                    server = Component.text(servers.get(i), TextColor.color(245, 221, 100));
                    server = server.hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT, Utils.line("Click " +
                            "to teleport to " + servers.get(i))));
                    server = server.clickEvent(ClickEvent.runCommand("/server " + servers.get(i)));
                }

                message = message.append(server);

                // If it's the last item, don't add a comma.
                if (i < (servers.size() - 1)) {

                    message = message.append(Utils.line(", "));
                }
            }

            player.sendMessage(message);
        } else {

            // Teleport to the server of the first arg.
            // If that is not a server notify the player.
            if (globalSQL.hasRow("SELECT name FROM server_data WHERE name='" + args[0] + "';")) {

                // Check if the player is not already on this server.
                if (Objects.equals(constants.serverName(), args[0])) {

                    player.sendMessage(ChatUtils.error("You are already on this server."));
                } else {

                    serverAPI.switchServer(PlayerAdapter.adapt(player), args[0]);
                }
            } else {

                player.sendMessage(ChatUtils.error("The server ").append(Component.text(args[0],
                        NamedTextColor.DARK_RED).append(ChatUtils.error(" does not exist!"))));
            }
        }
    }

    @Override
    public String getLabel() {
        return "server";
    }

    @Override
    public String getDescription() {
        return "Switch server by command.";
    }
}
