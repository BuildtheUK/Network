package net.bteuk.network.commands.tabcompleters;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class TreeTabCompleter extends AbstractTabCompleter{
    private final TabCompleterTree options;

    /**
     * Contructor
     *
     * @param options  the options that should be available in the TAB
     */
    public TreeTabCompleter(TabCompleterTree options) {
        this.options = options;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return getArgsforRoute(args);
    }

    private List<String> getArgsforRoute(String[] args)
    {
        return options.getNextPossibleStrings(args);
    }
}
