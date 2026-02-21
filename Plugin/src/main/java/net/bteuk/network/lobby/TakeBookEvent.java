package net.bteuk.network.lobby;

import net.bteuk.network.Network;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

public class TakeBookEvent implements Listener {

    public TakeBookEvent(Network instance) {
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void playerTakeLecternBook(PlayerTakeLecternBookEvent e) {
        // Always cancel, as there shouldn't be any other lecterns in the lobby.
        e.setCancelled(true);
    }
}
