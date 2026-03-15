package net.bteuk.network.proxy;

import org.btuk.proxy.core.player.Player;
import org.btuk.proxy.core.player.PlayerManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkPlayerManager implements PlayerManager {

    private final JavaPlugin plugin;

    public NetworkPlayerManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<Player> getPlayers() {
        return plugin.getServer().getOnlinePlayers().stream().map(NetworkPlayer::new).collect(Collectors.toCollection(ArrayList::new));
    }
}
