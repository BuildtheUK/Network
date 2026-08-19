package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Listener class that implements a generic clickable item in the inventory of the player.
 * It can not be (re)moved from the inventory at any point in time.
 */
@Log
public class ClickableItemListener implements Listener {

    private final Network instance;
    private final ItemStack item;
    private final ClickItemAction clickAction;

    public ClickableItemListener(Network instance, ItemStack item, ClickItemAction action) {

        this.instance = instance;
        this.item = item;
        this.clickAction = action;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    public void unregister() {
        PlayerInteractEvent.getHandlerList().unregister(this);
        InventoryClickEvent.getHandlerList().unregister(this);
        PlayerSwapHandItemsEvent.getHandlerList().unregister(this);
        PlayerDropItemEvent.getHandlerList().unregister(this);
        InventoryDragEvent.getHandlerList().unregister(this);
    }

    private NetworkUser getUserIfExists(Player p) {
        NetworkUser u = instance.getUser(p);
        if (u == null) {
            log.severe("User " + p.getName() + " can not be found!");
            p.sendMessage(ChatUtils.error("User can not be found, please relog!"));
        }
        return u;
    }

    private void clickActionIfCorrectItem(Cancellable cancellableEvent, Player player, ItemStack clickedItem) {
        NetworkUser user = getUserIfExists(player);

        if (item.isSimilar(clickedItem)) {
            cancellableEvent.setCancelled(true);
            // Run click action if user not null.
            if (user != null) {
                clickAction.click(user);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        clickActionIfCorrectItem(e, e.getPlayer(), e.getItem());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        clickActionIfCorrectItem(e, (Player) e.getWhoClicked(), e.getCurrentItem());
        if (!e.isCancelled()) {
            clickActionIfCorrectItem(e, (Player) e.getWhoClicked(), e.getCursor());
        }
    }

    @EventHandler
    public void swapHands(PlayerSwapHandItemsEvent e) {
        clickActionIfCorrectItem(e, e.getPlayer(), e.getMainHandItem());
        if (!e.isCancelled()) {
            clickActionIfCorrectItem(e, e.getPlayer(), e.getOffHandItem());
        }
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent e) {
        clickActionIfCorrectItem(e, e.getPlayer(), e.getItemDrop().getItemStack());
    }

    @EventHandler
    public void moveItem(InventoryDragEvent e) {
        clickActionIfCorrectItem(e, (Player) e.getWhoClicked(), e.getOldCursor());
        if (!e.isCancelled()) {
            clickActionIfCorrectItem(e, (Player) e.getWhoClicked(), e.getCursor());
        }
    }

    @EventHandler
    public void creativeItem(InventoryCreativeEvent e) {
        clickActionIfCorrectItem(e, (Player) e.getWhoClicked(), e.getCurrentItem());
        if (!e.isCancelled()) {
            clickActionIfCorrectItem(e, (Player) e.getWhoClicked(), e.getCursor());
        }
    }

    public interface ClickItemAction {
        void click(NetworkUser u);
    }
}
