package net.bteuk.network.eventing.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class SingleMessageChatListener implements Listener {

    private final BukkitTask task;

    private final ChatEventHandler eventHandler;

    public SingleMessageChatListener(JavaPlugin plugin, Player player, Component cancelledMessage, long timeoutTicks, ChatEventHandler eventHandler) {

        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        this.eventHandler = eventHandler;

        // Start scheduled task to cancel the listener after a timeout.
        task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Send message to player telling them it's been timed out.
            if (player != null && player.isOnline()) {
                player.sendMessage(cancelledMessage);
            }
            unregister();
        }, timeoutTicks);
    }

    @EventHandler
    public void ChatEvent(AsyncChatEvent e) {
        if (eventHandler.handleChatEvent(e)) {
            task.cancel();
            unregister();
        }
    }

    public void unregister() {
        AsyncChatEvent.getHandlerList().unregister(this);
    }
}
