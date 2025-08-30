package net.bteuk.network.commands.tabcompleters;

import net.bteuk.network.api.SQLAPI;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class LocationAndSubcategorySelector extends AbstractTabCompleter {

    private final SQLAPI globalSQL;
    private final int argIndex;

    /**
     * Contructor
     *
     * @param argIndex the index for which the tab completer should be.
     */
    public LocationAndSubcategorySelector(SQLAPI globalSQL, int argIndex) {
        this.globalSQL = globalSQL;
        this.argIndex = argIndex;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // Get an array of locations.
        List<String> locations = globalSQL.getStringList("SELECT location FROM " +
                "location_data;");

        // Add subcategories.
        locations.addAll(globalSQL.getStringList("SELECT name FROM location_subcategory"));
        return onTabCompleteArg(args, locations, argIndex);
    }
}
