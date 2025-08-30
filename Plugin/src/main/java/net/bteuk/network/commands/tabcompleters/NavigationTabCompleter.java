package net.bteuk.network.commands.tabcompleters;

import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.utils.enums.Category;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class NavigationTabCompleter extends AbstractTabCompleter {

    private final SQLAPI globalSQL;

    public NavigationTabCompleter(SQLAPI globalSQL) {
        this.globalSQL = globalSQL;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        Collection<String> result = new ArrayList<>();
        if (args.length > 0) {
            switch (args[0].toUpperCase()) {

                // If arg[0] is remove, update or suggested, then use locations as arg[1] selector.
                case "UPDATE", "REMOVE", "SUGGESTED" -> result.addAll(LocationSelector.locationSelectorOnArg(globalSQL, args, 1));

                // If arg[0] is subcategory then the selector depends on the next argument.
                case "SUBCATEGORY" -> {
                    // If arg[1] is add then list all categories for arg[2].
                    // If arg[1] is remove then give all subcategories for arg[2].
                    if (args.length > 1 && args[1].equalsIgnoreCase("add")) {
                        result.addAll(onTabCompleteArg(args,
                                Arrays.stream(Category.values()).filter(Category::isSelectable).map(Category::toString)
                                        .collect(Collectors.toList()), 2));
                    } else if (args.length > 1 && args[1].equalsIgnoreCase("remove")) {
                        result.addAll(onTabCompleteArg(args, globalSQL.getStringList(
                                "SELECT name FROM location_subcategory"), 2));
                    }
                }
            }
        }
        return result;
    }
}
