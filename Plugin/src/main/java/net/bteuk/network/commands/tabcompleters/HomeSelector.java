package net.bteuk.network.commands.tabcompleters;

import net.bteuk.network.api.SQLAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class HomeSelector extends AbstractTabCompleter {

    private final SQLAPI globalSQL;

    public HomeSelector(SQLAPI globalSQL) {
        this.globalSQL = globalSQL;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> homes =
                globalSQL.getStringList(
                        "SELECT name FROM home WHERE uuid='" + ((Player) sender).getUniqueId() + "' AND name IS NOT " +
                                "NULL;");

        return onTabCompleteArg(args, homes, 0);
    }
}
