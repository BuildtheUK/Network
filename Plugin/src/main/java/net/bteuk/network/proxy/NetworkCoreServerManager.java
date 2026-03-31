package net.bteuk.network.proxy;

import org.btuk.proxy.core.server.CoreServerManager;
import org.btuk.proxy.core.server.Server;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class NetworkCoreServerManager implements CoreServerManager {

    private final NetworkServer server;

    public NetworkCoreServerManager(JavaPlugin plugin) {
        this.server = new NetworkServer(plugin.getServer());
    }

    @Override
    public Server createServer(String name) {
        return server;
    }

    @Override
    public Optional<Server> getServer(String name) {
        return Optional.of(server);
    }

    @Override
    public Set<Server> getServers() {
        return Set.of(server);
    }

    @Override
    public List<Server> getOnlineServers() {
        return List.of(server);
    }

    @Override
    public void addServer(Server server) {
        // Do nothing.
    }

    @Override
    public void removeServer(Server server) {
        // Do nothing.
    }

    @Override
    public void shutdown() {
        // Do nothing.
    }
}
