package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.core.Time;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

@Log
public class NetworkMoveListener implements Listener {

    private final Network instance;

    private final Afk afk;

    private boolean blocked;

    public NetworkMoveListener(Network plugin, Afk afk) {

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        this.instance = plugin;
        this.afk = afk;

        blocked = false;
    }

    public void block() {
        blocked = true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {

        if (blocked) {
            e.setCancelled(true);
            return;
        }

        // If u is null, cancel; this could be the case when moving before the user instance has been created.
        NetworkUser user = instance.getUser(e.getPlayer());
        if (user == null) {
            e.setCancelled(true);
            return;
        }

        // Cancel event if player is switching server.
        if (user.isSwitching()) {
            e.setCancelled(true);
            return;
        }

        // Reset last movement of player, if they're afk unset that.
        user.last_movement = Time.currentTime();
        if (user.isAfk()) {
            user.setAfk(false);
            afk.updateAfkStatus(user, false);
        }
    }
}
