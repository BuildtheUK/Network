package net.bteuk.network.chat.bypass;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.entity.ShutdownHook;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

@Log
public class CommandBookListener implements Listener, ShutdownHook {

    private final Network plugin;

    /**
     * Constructs the object, gets the book ready
     *
     * @param plugin A reference to the instance of the TeachingTutorials plugin
     */
    public CommandBookListener(Network plugin) {
        this.plugin = plugin;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void BookCloseEvent(PlayerEditBookEvent event) {
        // Check if the current item in the hand of the player is the command book.
        ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
        if (!CommandBook.isCommandBook(plugin, heldItem)) {
            // It's also possible that the book was in their offhand, so also check that.
            heldItem = event.getPlayer().getInventory().getItemInOffHand();
            if (!CommandBook.isCommandBook(plugin, heldItem)) {
                return;
            }
        }
        event.setCancelled(true);

        // Attempt to run the command if one was entered.
        runBookClose(event.getPlayer(), event.getNewBookMeta());

        // Update the book content (delay this slightly to ensure command feedback has been added to the message history).
        Bukkit.getScheduler().runTaskLater(plugin, () -> CommandBook.updateCommandBook(plugin, event.getPlayer()), 1L);
    }

    @EventHandler
    public void bookDropped(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (CommandBook.isCommandBook(plugin, item)) {
            event.setCancelled(true);
        }
    }

    private void runBookClose(Player player, BookMeta bookMeta) {
        if (bookMeta.pages().isEmpty()) {
            return;
        }

        String plainFirstPage = PlainTextComponentSerializer.plainText().serialize(bookMeta.page(1));

        // Instructions and separator are on the first page.
        // We look for the 2nd / and take everything after it.
        String[] parts = plainFirstPage.split("/", 3);
        String command = parts.length == 3 ? parts[2].trim() : "";

        log.info("Command: " + command);
        if (!command.isEmpty()) {
            // Dispatch as the player
            Bukkit.dispatchCommand(player, command);
        }
    }

    @Override
    public void shutdown() {
        HandlerList.unregisterAll(this);
    }
}
