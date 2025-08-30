package net.bteuk.network.commands;

import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.commands.tabcompleters.MultiArgSelector;
import net.bteuk.network.commands.tabcompleters.PlayerSelector;
import net.bteuk.network.utils.Role;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of promote and denmote. The only difference between the 2 is the boolean that indicates whether
 * it's a promotion or demotion.
 */
public abstract class PromotionAction extends AbstractCommand {

    private final Component error;
    private final Roles roles;
    private final Network instance;
    private final CustomChat chat;

    protected PromotionAction(Network instance, Roles roles, CustomChat customChat, Component error) {
        this.instance = instance;
        this.roles = roles;
        this.error = error;
        this.chat = customChat;
        setTabCompleter(new MultiArgSelector(List.of(new PlayerSelector(instance, false),
                new FixedArgSelector(roles.getRoles().stream().map(Role::getId).collect(Collectors.toList()), 1))));
    }

    public void onCommand(CommandSender sender, String[] args, boolean demote) {
        // Get the uuid of the player.
        if (args.length != 2) {
            sender.sendMessage(error);
            return;
        }

        // Search for the uuid of the player.
        // Also retrieve the name, as it is possible the cases aren't correct.
        String uuid = instance.getGlobalSQL().getString("SELECT uuid FROM player_data WHERE name='" + args[0] + "';");
        String name = instance.getGlobalSQL().getString("SELECT name FROM player_data WHERE name='" + args[0] + "';");
        if (uuid == null) {
            sender.sendMessage(error);
            return;
        }

        CompletableFuture<Component> resultFuture = roles.alterRole(uuid, name, args[1], demote, false, chat);
        Executors.newSingleThreadExecutor().submit(() -> resultFuture.thenAcceptAsync(sender::sendMessage).join());
    }
}
