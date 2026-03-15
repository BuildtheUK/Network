package net.bteuk.network.building_counter;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.bteuk.network.commands.Buildings;
import net.bteuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class ConfirmationListener implements Listener {

    private final Buildings buildings;
    private final Location location;
    private final Player player;
    private final BukkitTask timeoutTask;

    public ConfirmationListener(Buildings buildings, Location location, Player player, Plugin plugin) {
        this.buildings = buildings;
        this.location = location;
        this.player = player;
        buildings.addPlayerToListenerList(player);
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        timeoutTask = plugin.getServer().getScheduler().runTaskLater(plugin, this::timeout, 20 * 120);
    }

    @EventHandler
    public void chatEvent(AsyncChatEvent e) {
        if (e.getPlayer().equals(player)) {
            timeoutTask.cancel();
            e.getHandlers().unregister(this);
            buildings.removePlayerFromListenerList(player);
            e.setCancelled(true);
            if (((net.kyori.adventure.text.TextComponent) e.message()).content().equals("y")) {
                buildings.addBuildingToDataBase(e.getPlayer(), location);
            } else {
                e.getPlayer().sendMessage(ChatUtils.error("No building added"));
            }
        }
    }

    private void timeout() {
        AsyncChatEvent.getHandlerList().unregister(this);
        buildings.removePlayerFromListenerList(player);
        player.sendMessage(ChatUtils.error("Confirmation timed out. No building added."));
    }
}
