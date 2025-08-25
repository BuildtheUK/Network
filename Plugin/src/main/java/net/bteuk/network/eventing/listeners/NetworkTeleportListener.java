package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

@Log
public class NetworkTeleportListener implements Listener {

    private final Network instance;
    private boolean blocked;

    public NetworkTeleportListener(Network instance) {
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
        this.instance = instance;
        blocked = false;
    }

    public void block() {
        blocked = true;
    }

    @Deprecated
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {

        if (blocked || e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            e.setCancelled(true);
            if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
                e.getPlayer().sendMessage(ChatUtils.error("Teleporting via the spectator menu is disabled, please use" +
                        " /tp <player>"));
            }
            return;
        }

        Player p = e.getPlayer();
        NetworkUser u = instance.getUser(p);

        // If u is null, cancel.
        if (u == null) {
            log.severe("User " + p.getName() + " can not be found!");
            p.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            e.setCancelled(true);
            return;
        }

        // Cancel event if player is switching server.
        if (u.isSwitching()) {
            e.setCancelled(true);
            return;
        }

        // If the building companion is enabled, check if the player changed to a different world.
        if (u.getCompanion() != null) {
            u.getCompanion().checkChangeWorld(e.getTo().getWorld());
        }
    }
}
