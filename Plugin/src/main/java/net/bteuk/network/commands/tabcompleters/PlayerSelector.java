package net.bteuk.network.commands.tabcompleters;

import net.bteuk.network.Network;
import net.bteuk.network.lib.dto.OnlineUser;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Online player name selector for the 1st command argument.
 */
public class PlayerSelector extends AbstractTabCompleter {

    private final Network instance;
    private final int argIndex;

    private final boolean excludeSelf;

    public PlayerSelector(Network instance) {
        this(instance, 0, true);
    }

    public PlayerSelector(Network instance, boolean excludeSelf) {
        this(instance, 0, excludeSelf);
    }

    public PlayerSelector(Network instance, int argIndex, boolean excludeSelf) {
        this.instance = instance;
        this.argIndex = argIndex;
        this.excludeSelf = excludeSelf;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // Get an array of online players, excluding yourself.
        List<String> names = instance.getOnlineUsers().stream().map(OnlineUser::getName).collect(Collectors.toList());
        if (excludeSelf && (sender instanceof Player p)) {
            names.remove(p.getName());
        }
        return onTabCompleteArg(args, names, argIndex);
    }
}
