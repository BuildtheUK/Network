package net.bteuk.network.regions.listener;

import net.bteuk.network.regions.PlayerEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ServerQuitListener implements Listener {

    private final PlayerEvent onServerQuit;

    public ServerQuitListener(JavaPlugin plugin, PlayerEvent onServerQuit) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.onServerQuit = onServerQuit;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        onServerQuit.playerEvent(event.getPlayer());
    }
}
