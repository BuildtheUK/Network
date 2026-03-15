package net.bteuk.network.eventing.listeners.staff;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.Getter;
import net.bteuk.network.Network;
import net.bteuk.network.gui.staff.ModerationActionGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.Locale;

import static net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;

public class ModerationReasonListener implements Listener {

    private final Network instance;
    private final NetworkUser u;
    private final ModerationActionGui gui;

    @Getter
    private final BukkitTask task;

    public ModerationReasonListener(Network instance, NetworkUser u, ModerationActionGui gui) {
        this.instance = instance;
        this.u = u;
        this.gui = gui;

        Bukkit.getServer().getPluginManager().registerEvents(this, instance);

        // Start timer to automatically close the listener.
        task = Bukkit.getScheduler().runTaskLater(instance, () -> {
            // Send message to player telling them it's been timer out.
            if (u.player != null) {
                u.player.sendMessage(ChatUtils.error("'Set " + gui.getType().label.toLowerCase(Locale.ROOT) + " " +
                        "reason' cancelled."));
            }
            unregister();
        }, 1200L);
    }

    @EventHandler
    public void ChatEvent(AsyncChatEvent e) {

        // Check if this is the correct player.
        if (e.getPlayer().equals(u.player)) {

            e.setCancelled(true);

            // Check if message is 256 characters or less.
            if (PlainTextComponentSerializer.plainText().serialize(e.message()).length() > 64) {

                e.getPlayer().sendMessage(ChatUtils.error("The region tag can't be longer than 256 characters, please" +
                        " try again."));
            } else {

                // Set the reason.
                gui.setReason(PlainTextComponentSerializer.plainText().serialize(e.message()));
                e.getPlayer().sendMessage(ChatUtils.success("Set reason to: ")
                        .append(e.message().color(DARK_AQUA)));

                // Refresh and reopen the gui.
                // This also cancels the task and unregisters the listener.
                gui.refresh();

                Bukkit.getScheduler().runTask(instance, () -> gui.open(u.player));
            }
        }
    }

    public void unregister() {
        AsyncChatEvent.getHandlerList().unregister(this);
    }
}
