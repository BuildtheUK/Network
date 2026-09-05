package net.bteuk.network.regions.listener;

import net.bteuk.network.regions.PlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class ServerJoinListener implements Listener {

    private static final Vector ZERO_VELOCITY = new Vector(0, 0, 0);

    private final JavaPlugin plugin;
    private final PlayerEvent onServerJoin;

    public ServerJoinListener(JavaPlugin plugin, PlayerEvent onServerJoin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.onServerJoin = onServerJoin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Block player velocity that may have been saved from the previous session.
        Player player = event.getPlayer();
        player.setVelocity(ZERO_VELOCITY);
        player.setFallDistance(0.0F);
        player.teleport(player.getLocation());

        // Reset velocity again after 1 tick to ensure the client-side velocity is cleared.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.setVelocity(ZERO_VELOCITY);
            }
        }, 1L);

        onServerJoin.playerEvent(player);
    }
}
