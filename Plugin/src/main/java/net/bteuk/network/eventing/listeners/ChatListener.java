package net.bteuk.network.eventing.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.core.Time;
import net.bteuk.network.exceptions.NotMutedException;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.staff.Moderation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static net.bteuk.network.CustomChat.getChatMessage;

@Log
public class ChatListener implements Listener {

    private final Network instance;
    private final Moderation moderation;
    private final Afk afk;
    private final MessageSender messageSender;

    public ChatListener(Network instance, Moderation moderation, Afk afk, MessageSender messageSender) {
        this.instance = instance;
        this.moderation = moderation;
        this.afk = afk;
        this.messageSender = messageSender;

        instance.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatEvent(AsyncChatEvent e) {

        // If player is muted cancel.
        if (moderation.isMuted(e.getPlayer().getUniqueId().toString())) {
            e.setCancelled(true);
            try {

                // Send a message and end an event.
                e.getPlayer().sendMessage(moderation.getMutedComponent(e.getPlayer().getUniqueId().toString()));
                return;
            } catch (NotMutedException ex) {

                // Unset the muted status.
                e.setCancelled(false);
            }
        }

        if (!e.isCancelled()) {
            e.setCancelled(true);
            // Get user, if staff chat is enabled, send the message to staff chat.
            NetworkUser user = instance.getUser(e.getPlayer());

            // If u is null, cancel.
            if (user == null) {
                log.severe("User " + e.getPlayer().getName() + " can not be found!");
                e.getPlayer().sendMessage(ChatUtils.error("User can not be found, please relog!"));
                return;
            }

            // Reset last movement of player, if they're afk unset that.
            user.last_movement = Time.currentTime();

            if (user.isAfk()) {
                user.setAfk(false);
                afk.updateAfkStatus(user, false);
            }
            ChatMessage chatMessage = getChatMessage(e.message(), user);
            messageSender.sendSocketMessage(chatMessage);
        }
    }
}
