package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Setter;
import net.bteuk.network.commands.tabcompleters.TabCompleter;
import net.bteuk.network.lib.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class for registering a command.
 * The implementation of the commandExecutor happens in the extending class.
 */
public abstract class AbstractCommand implements BasicCommand {

    protected static final Component COMMAND_ONLY_BY_PLAYER = ChatUtils.error("This command can only be run by a " +
            "player.");
    protected static final Component NO_PERMISSION = ChatUtils.error("You do not have permission to use this command.");

    @Setter
    protected TabCompleter tabCompleter;

    @Override
    public Collection<String> suggest(CommandSourceStack commandSourceStack, String[] args) {
        if (tabCompleter != null) {
            return tabCompleter.onTabComplete(commandSourceStack.getSender(), args);
        }
        return Collections.emptyList();
    }

    public abstract String getLabel();

    public  abstract String getDescription();

    public  List<String> getAliases() {
        return Collections.emptyList();
    }

    /**
     * Gets the {@link Player} from the server, if the sender is not a player send them a warning with
     * 'This command can only be run by a player.'
     *
     * @param stack the command source stack
     * @return the {@link Player} instance, or null if not a player
     */
    protected Player getPlayer(CommandSourceStack stack) {

        // Check if the sender is a player.
        CommandSender sender = stack.getSender();
        if (!(sender instanceof Player p)) {

            sender.sendMessage(COMMAND_ONLY_BY_PLAYER);
            return null;
        }

        return p;
    }

    /**
     * Checks whether the sender has permission, if the sender is not a {@link Player} then this will always return
     * true.
     * If the player doesn't have the permission send a no permission message.
     *
     * @param sender     the command sender
     * @param permission the permission to check
     * @return whether the sender has the permission
     */
    protected boolean hasPermission(CommandSender sender, String permission) {
        if (sender instanceof Player p) {
            if (!p.hasPermission(permission)) {
                sender.sendMessage(NO_PERMISSION);
                return false;
            }
        }
        return true;
    }
}
