package net.bteuk.network.eventing.listeners;

import com.sk89q.worldguard.bukkit.ProtectionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import io.papermc.paper.event.entity.EntityBreakByEntityEvent;
import lombok.extern.java.Log;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

@Log
public class CushionProtector implements Listener {

    public CushionProtector(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCushionDamage(EntityBreakByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.CUSHION) {
            return;
        }

        if (event.getDamageSource().getCausingEntity() instanceof Player player) {
            ProtectionQuery query = WorldGuardPlugin.inst().createProtectionQuery();
            if (!query.testEntityDestroy(player, entity)) {
                log.info("Cushion break blocked, player: " + player.getName());
                event.setCancelled(true);
            }
        } else {
            log.info("Cushion break blocked, damage source: " + event.getDamageSource().getCausingEntity());
            event.setCancelled(true);
        }
    }
}
