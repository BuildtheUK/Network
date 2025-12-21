package net.bteuk.network.commands.tabcompleters;

import net.bteuk.network.Network;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;

public class ConditionalPlayerSelector extends PlayerSelector {

    private final Predicate<String[]> condition;

    public ConditionalPlayerSelector(Network instance, int argIndex, Predicate<String[]> predicate) {
        super(instance, argIndex, false);
        this.condition = predicate;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (condition.test(args) && sender.hasPermission("uknet.nick.reset.others")) {
            return super.onTabComplete(sender, args);
        } else {
            return Collections.emptyList();
        }
    }
}
