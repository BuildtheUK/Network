package net.bteuk.network.proxy;

import net.bteuk.network.core.Time;
import org.btuk.proxy.core.player.Player;
import org.btuk.proxy.core.server.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkServer implements Server {

    private final org.bukkit.Server bukkitServer;

    public NetworkServer(org.bukkit.Server bukkitServer) {
        this.bukkitServer = bukkitServer;
    }

    @Override
    public List<Player> getPlayers() {
        return bukkitServer.getOnlinePlayers().stream().map(NetworkPlayer::new).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public String getName() {
        return bukkitServer.getName();
    }

    @Override
    public boolean canPing() {
        return true; // The server is always available, else this code is not accessible.
    }

    @Override
    public void setLastPing(long time) {
        // Do nothing.
    }

    @Override
    public long getLastPing() {
        return Time.currentTime();
    }
}
