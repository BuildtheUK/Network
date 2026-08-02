package net.bteuk.network.lobby;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.kyori.adventure.text.Component;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LobbyCommand extends AbstractCommand {

    private static final Component INVALID_FORMAT = ChatUtils.error("/lobby reload portals");

    private final Lobby lobby;
    private final Constants constants;

    public LobbyCommand(Lobby lobby, Constants constants) {
        this.lobby = lobby;
        this.constants = constants;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check permission if player, or if the server is the lobby.
        CommandSender sender = stack.getSender();
        if (!sender.hasPermission("uknet.lobby.reload") || constants.serverType() != ServerType.LOBBY) {
            if (sender instanceof Player p) {
                p.performCommand("spawn");
            }
            return;
        }

        // Check args.
        if (args.length < 2 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(INVALID_FORMAT);
        } else if (args[1].equalsIgnoreCase("portals")) {

            if (!hasPermission(sender, "uknet.lobby.reload.portals")) {
                return;
            }

            lobby.reloadPortals();
            sender.sendMessage(ChatUtils.success("Reloaded portals"));
        } else {
            sender.sendMessage(INVALID_FORMAT);
        }
    }

    @Override
    public String getLabel() {
        return "lobby";
    }

    @Override
    public String getDescription() {
        return "Command for all lobby management.";
    }
}
