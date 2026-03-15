package net.bteuk.network.commands.tabcompleters;

import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract tab completer, provides the logic for getting the correct arguments based on a list of options and
 * argument index using the provided input.
 */
public abstract class AbstractTabCompleter implements TabCompleter {

    public static List<String> onTabCompleteArg(String[] inputArgs, List<String> options, int argIndex) {

        // Return list.
        List<String> returns = new ArrayList<>();

        if (inputArgs.length == (argIndex + 1)) {
            StringUtil.copyPartialMatches(inputArgs[argIndex], options, returns);
        } else if (inputArgs.length == 0 && argIndex == 0) {
            return options;
        }
        return returns;
    }
}