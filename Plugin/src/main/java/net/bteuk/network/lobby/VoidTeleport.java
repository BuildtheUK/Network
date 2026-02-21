package net.bteuk.network.lobby;

import net.bteuk.network.Network;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class VoidTeleport implements Listener {

    private final Lobby lobby;

    public VoidTeleport(Network instance, Lobby lobby) {
        this.lobby = lobby;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        if (e.getTo().getY() < 0) {
            // Teleport to the spawn-point.
            e.getPlayer().teleport(lobby.getSpawn());
        }
    }
}
