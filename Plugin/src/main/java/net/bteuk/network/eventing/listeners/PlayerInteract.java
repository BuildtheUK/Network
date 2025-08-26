package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.commands.Navigator;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@Log
public class PlayerInteract implements Listener {

    private final Network instance;
    private final Navigator navigator;

    public PlayerInteract(Network instance, Navigator navigator) {
        this.instance = instance;
        this.navigator = navigator;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {

        NetworkUser u = instance.getUser(e.getPlayer());

        // If u is null, cancel.
        if (u == null) {
            log.severe("User " + e.getPlayer().getName() + " can not be found!");
            e.getPlayer().sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        if (e.getItem() != null) {
            if (e.getItem().equals(instance.navigatorItem)) {
                e.setCancelled(true);
                // Open navigator.
                navigator.openNavigator(u);
            }
        }
    }

    // If the player clicks on the navigator in their inventory, open the gui.
    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (e.getCurrentItem() == null) {
            return;
        }

        NetworkUser u = instance.getUser((Player) e.getWhoClicked());

        // If u is null, cancel.
        if (u == null) {
            log.severe("User " + e.getWhoClicked().getName() + " can not be found!");
            e.getWhoClicked().sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        // If item is navigator then open it.
        if (e.getCurrentItem().equals(instance.navigatorItem)) {
            e.setCancelled(true);

            // If item is not in slot 8, delete it.
            if (e.getSlot() != 8) {
                u.player.getInventory().clear(e.getSlot());
                return;
            }

            u.player.closeInventory();
            Bukkit.getScheduler().runTaskLater(instance, () -> navigator.openNavigator(u), 1);
        }
    }


    /*
    The following events are to prevent the navigator being moved in the inventory,
    causing duplicate items which are difficult to remove.
     */
    @EventHandler
    public void swapHands(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem().equals(instance.navigatorItem)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().equals(instance.navigatorItem)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dragItem(InventoryMoveItemEvent e) {
        if (e.getItem().equals(instance.navigatorItem)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dragItem(InventoryDragEvent e) {
        if (e.getOldCursor().equals(instance.navigatorItem)) {
            e.setCancelled(true);
        }

        if (e.getCursor() == null) {
            return;
        }

        if (e.getCursor().equals(instance.navigatorItem)) {
            e.setCancelled(true);
        }
    }
}
