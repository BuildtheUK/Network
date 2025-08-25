package net.bteuk.network.regions.listener;

import net.bteuk.network.regions.PlayerEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerJoinListener implements Listener {

    private final PlayerEvent onServerJoin;

    public ServerJoinListener(JavaPlugin plugin, PlayerEvent onServerJoin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.onServerJoin = onServerJoin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        onServerJoin.playerEvent(event.getPlayer());
    }
}
