package net.bteuk.network.commands.tabcompleters;

import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.core.Constants;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class ServerSelector extends AbstractTabCompleter {

    private final SQLAPI globalSQL;
    private final Constants constants;

    public ServerSelector(SQLAPI globalSQL, Constants constants) {
        this.globalSQL = globalSQL;
        this.constants = constants;
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        // Get an array of servers, excluding the current server.
        List<String> servers = globalSQL.getStringList("SELECT name FROM server_data " +
                "WHERE server<>'" + constants.serverName() + ";");

        return onTabCompleteArg(args, servers, 0);
    }
}
