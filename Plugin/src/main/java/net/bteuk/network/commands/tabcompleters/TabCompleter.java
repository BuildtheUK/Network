package net.bteuk.network.commands.tabcompleters;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@FunctionalInterface
public interface TabCompleter {

    @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args);
}
