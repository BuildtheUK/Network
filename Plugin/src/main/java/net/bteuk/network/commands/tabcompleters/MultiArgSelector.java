package net.bteuk.network.commands.tabcompleters;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MultiArgSelector extends AbstractTabCompleter {

    private final List<AbstractTabCompleter> tabCompleters;

    public MultiArgSelector(List<AbstractTabCompleter> tabCompleters) {
        this.tabCompleters = tabCompleters;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        Collection<String> result = new ArrayList<>();
        for (AbstractTabCompleter tabCompleter : tabCompleters) {
            result.addAll(tabCompleter.onTabComplete(sender, args));
        }
        return result;
    }
}
