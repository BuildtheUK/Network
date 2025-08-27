package net.bteuk.network.eventing.listeners.navigation;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.bteuk.network.Network;
import net.bteuk.network.gui.navigation.AddLocation;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.enums.AddLocationType;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class LocationNameListener implements Listener {

    private final AddLocation gui;
    private final Player p;

    private final BukkitTask task;

    public LocationNameListener(Player p, AddLocation gui) {

        Bukkit.getServer().getPluginManager().registerEvents(this, Network.getInstance());

        this.p = p;
        this.gui = gui;

        // Start timer to automatically close the listener.
        task = Bukkit.getScheduler().runTaskLater(Network.getInstance(), () -> {
            // Send message to player telling them it's been timer out.
            if (p != null) {
                p.sendMessage(ChatUtils.error("'Set Location Name' cancelled."));

                // If AddLocation gui still exists, reopen it.
                // Also check if player is actually still online.
                if (p.isOnline()) {
                    NetworkUser u = Network.getInstance().getUser(p);
                    // Open staff gui if it's update or review.
                    if (gui.getType() == AddLocationType.ADD) {
                        if (Objects.requireNonNull(u).mainGui != null) {
                            if (u.mainGui instanceof AddLocation) {
                                u.mainGui.open(u.player);
                            }
                        }
                    } else {
                        if (Objects.requireNonNull(u).staffGui != null) {
                            if (u.staffGui instanceof AddLocation) {
                                u.staffGui.open(u.player);
                            }
                        }
                    }
                }
            }
            unregister();
        }, 1200L);
    }

    @EventHandler
    public void ChatEvent(AsyncChatEvent e) {

        // Check if this is the correct player.
        if (e.getPlayer().equals(p)) {

            e.setCancelled(true);

            // Check if message is under 64 character.
            if (PlainTextComponentSerializer.plainText().serialize(e.message()).length() > 64) {
                e.getPlayer().sendMessage(ChatUtils.error("The location name can't be longer than 64 characters."));
            } else {

                // Set location name.
                gui.setName(PlainTextComponentSerializer.plainText().serialize(e.message()));

                // Send message to player.
                p.sendMessage(ChatUtils.success("Set location name to ")
                        .append(e.message().color(NamedTextColor.DARK_AQUA)));

                // Unregister listener and task.
                task.cancel();
                unregister();

                // If AddLocation gui still exists, reopen it.
                NetworkUser u = Network.getInstance().getUser(p);
                if (gui.getType() == AddLocationType.ADD) {
                    if (Objects.requireNonNull(u).mainGui != null) {
                        if (u.mainGui instanceof AddLocation) {
                            Bukkit.getScheduler().runTask(Network.getInstance(), () -> {
                                u.mainGui.refresh();
                                u.mainGui.open(u.player);
                            });
                        }
                    }
                } else {
                    if (Objects.requireNonNull(u).staffGui != null) {
                        if (u.staffGui instanceof AddLocation) {
                            Bukkit.getScheduler().runTask(Network.getInstance(), () -> {
                                u.staffGui.refresh();
                                u.staffGui.open(u.player);
                            });
                        }
                    }
                }
            }
        }
    }

    public void unregister() {
        AsyncChatEvent.getHandlerList().unregister(this);
    }
}
