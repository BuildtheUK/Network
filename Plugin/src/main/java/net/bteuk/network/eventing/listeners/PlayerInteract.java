package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.minecraft.texteditorbooks.TextEditorBookListener;
import net.bteuk.network.Network;
import net.bteuk.network.chat.bypass.CommandBookCloseAction;
import net.bteuk.network.commands.Navigator;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
            if (e.getItem().equals(instance.getNavigatorItem())) {
                e.setCancelled(true);
                // Open navigator.
                navigator.openNavigator(u);
            } else if (e.getItem().equals(instance.getCommandBookItem())) {
                e.setCancelled(true);

                // Prepare history text in chunks for pagination
                java.util.List<String> pages = new java.util.ArrayList<>();
                // Page 1: Instructions and Command prompt
                pages.add("Write your command below the / and then sign or close the book to run it.\n\nTurn the page to view recent chat logs.\n\n/\n");

                StringBuilder historyPage = new StringBuilder();
                int lineCount = 0;
                for (net.kyori.adventure.text.Component msg : u.getMessageHistory()) {
                    // Check if adding this message would exceed roughly the page limit (approx 256 chars or 13-14 lines)
                    // Use legacy serialization to preserve colors
                    String legacyMsg = LegacyComponentSerializer.legacySection().serialize(msg);
                    String plainMsg = PlainTextComponentSerializer.plainText().serialize(msg);
                    if (historyPage.length() + plainMsg.length() > 250 || lineCount >= 13) {
                        pages.add(historyPage.toString());
                        historyPage = new StringBuilder();
                        lineCount = 0;
                    }
                    historyPage.append(legacyMsg).append("\n");
                    lineCount++;
                }
                if (historyPage.length() > 0) {
                    pages.add(historyPage.toString());
                }

                // Open a writable book editor for typing commands
                TextEditorBookListener listener =
                        new TextEditorBookListener(
                                instance,
                                u.player,
                                null,
                                "Command Book",
                                new CommandBookCloseAction(instance, u),
                                pages.toArray(new String[0])
                        );
                listener.startEdit("Command Book");
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
        if (e.getCurrentItem().equals(instance.getNavigatorItem())) {
            e.setCancelled(true);

            // If item is not in slot 8, delete it.
            if (e.getSlot() != 8) {
                u.player.getInventory().clear(e.getSlot());
                return;
            }

            u.player.closeInventory();
            Bukkit.getScheduler().runTaskLater(instance, () -> navigator.openNavigator(u), 1);
        } else if (e.getCurrentItem().equals(instance.getCommandBookItem())) {
            // If the inventory is not the player inventory, cancel it.
            if (e.getClickedInventory() == null || !e.getClickedInventory().equals(u.player.getInventory())) {
                e.setCancelled(true);
                return;
            }

            // If it's a shift-click, we should also check where it's going, but for simplicity, let's just cancel shift-clicks for now
            // to prevent it moving to a chest or other container.
            if (e.getClick().isShiftClick()) {
                e.setCancelled(true);
                return;
            }

            // If it's a right-click or other interaction that isn't just moving it, open the book.
            // But usually, InventoryClickEvent is for moving.
            // If the player is moving the item, we allow it as long as it stays in their inventory.
            // However, we still want Right-Click in inventory to open the book (consistent with Navigator).
            if (e.getClick().isRightClick()) {
                e.setCancelled(true);
                u.player.closeInventory();

                // Prepare history text in chunks for pagination
                java.util.List<String> pages = new java.util.ArrayList<>();
                // Page 1: Instructions and Command prompt
                pages.add("Write your command below the / and then sign or close the book to run it.\n\nTurn the page to view recent chat logs.\n\n/\n");

                StringBuilder historyPage = new StringBuilder();
                int lineCount = 0;
                for (net.kyori.adventure.text.Component msg : u.getMessageHistory()) {
                    String legacyMsg = LegacyComponentSerializer.legacySection().serialize(msg);
                    String plainMsg = PlainTextComponentSerializer.plainText().serialize(msg);
                    if (historyPage.length() + plainMsg.length() > 250 || lineCount >= 13) {
                        pages.add(historyPage.toString());
                        historyPage = new StringBuilder();
                        lineCount = 0;
                    }
                    historyPage.append(legacyMsg).append("\n");
                    lineCount++;
                }
                if (historyPage.length() > 0) {
                    pages.add(historyPage.toString());
                }

                Bukkit.getScheduler().runTaskLater(instance, () -> {
                    TextEditorBookListener listener =
                            new TextEditorBookListener(
                                    instance,
                                    u.player,
                                    null,
                                    "Command Book",
                                    new CommandBookCloseAction(instance, u),
                                    pages.toArray(new String[0])
                            );
                    listener.startEdit("Command Book");
                }, 1);
            }
        }
    }


    /*
    The following events are to prevent the navigator being moved in the inventory,
    causing duplicate items which are difficult to remove.
     */
    @EventHandler
    public void swapHands(PlayerSwapHandItemsEvent e) {
        if (e.getOffHandItem().equals(instance.getNavigatorItem()) || e.getOffHandItem().equals(instance.getCommandBookItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dropItem(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().equals(instance.getNavigatorItem()) || e.getItemDrop().getItemStack().equals(instance.getCommandBookItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dragItem(InventoryMoveItemEvent e) {
        if (e.getItem().equals(instance.getNavigatorItem()) || e.getItem().equals(instance.getCommandBookItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dragItem(InventoryDragEvent e) {
        if (e.getOldCursor().equals(instance.getNavigatorItem()) || e.getOldCursor().equals(instance.getCommandBookItem())) {
            e.setCancelled(true);
        }

        if (e.getCursor() == null) {
            return;
        }

        if (e.getCursor().equals(instance.getNavigatorItem()) || e.getCursor().equals(instance.getCommandBookItem())) {
            e.setCancelled(true);
        }
    }
}
