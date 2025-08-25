package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.gui.Gui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.UUID;

@Log
public class GuiListener implements Listener {

    private final Network instance;

    public GuiListener(Network instance) {

        this.instance = instance;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        UUID playerUUID = player.getUniqueId();

        UUID inventoryUUID = Gui.openInventories.get(playerUUID);

        if (inventoryUUID != null) {

            NetworkUser u = instance.getUser(player);

            // If u is null, cancel.
            if (u == null) {
                log.severe("User " + e.getWhoClicked().getName() + " can not be found!");
                e.getWhoClicked().sendMessage(ChatUtils.error("User can not be found, please relog!"));
                return;
            }

            e.setCancelled(true);
            Gui gui = Gui.inventoriesByUUID.get(inventoryUUID);
            Gui.guiAction action = gui.getActions().get(e.getRawSlot());

            if (action != null) {
                action.click(u);
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {

        Player p = (Player) e.getPlayer();
        UUID playerUUID = p.getUniqueId();

        // Get the uuid of the open inventory, if exists.
        UUID guiUuid = Gui.openInventories.get(playerUUID);

        if (guiUuid != null) {

            // Get the gui.
            Gui gui = Gui.inventoriesByUUID.get(guiUuid);

            // If the gui should delete on close, delete it.
            if (gui != null && gui.isDeleteOnClose()) {
                gui.delete();
            } else {
                // Remove the player from the list of open inventories.
                Gui.openInventories.remove(playerUUID);
            }
        }
    }
}
