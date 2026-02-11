package net.bteuk.network.commands.tabcompleters;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Will implement a TAB completer on the given argument using the list of options provided on initialisation.
 */
public class FixedArgSelector extends AbstractTabCompleter {

    private final List<String> options;

    private final int argIndex;

    /**
     * Contructor
     *
     * @param options  the options that should be available in the TAB completion on the first argument
     * @param argIndex the index for which the tab completer should be.
     */
    public FixedArgSelector(List<String> options, int argIndex) {
        this.options = options;
        this.argIndex = argIndex;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        return onTabCompleteArg(args, options, argIndex);
    }
}
