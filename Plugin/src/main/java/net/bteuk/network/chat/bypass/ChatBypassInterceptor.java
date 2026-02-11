package net.bteuk.network.chat.bypass;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Intercepts outgoing chat/system messages to players.
 * When a player has command book chat bypass enabled, the original chat packet
 * is cancelled and the content is shown via action bar instead, while also
 * being recorded into the user's message history buffer.
 */
public class ChatBypassInterceptor {

    private final Network instance;

    public ChatBypassInterceptor(Network instance) {
        this.instance = instance;
        register();
    }

    private void register() {
        ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        pm.addPacketListener(new PacketAdapter(instance, ListenerPriority.NORMAL,
                PacketType.Play.Server.CHAT,
                PacketType.Play.Server.SYSTEM_CHAT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                NetworkUser user = instance.getUser(player);
                if (user == null || !user.isCommandBookEnabled()) {
                    return; // let it through normally
                }

                PacketContainer packet = event.getPacket();
                Component messageComponent = extractComponent(packet);
                if (messageComponent == null) {
                    return; // nothing to do
                }

                // Record to history with simple size cap of 50
                // We record history in reverse order (newest first) to display correctly in the book
                List<Component> history = user.getMessageHistory();
                history.add(0, messageComponent);
                if (history.size() > 50) {
                    history.remove(history.size() - 1);
                }

                // Show latest via action bar and cancel the original
                player.sendActionBar(messageComponent);
                event.setCancelled(true);
            }
        });
    }

    private Component extractComponent(PacketContainer packet) {
        try {
            // Prefer chat components if available
            List<WrappedChatComponent> comps = packet.getChatComponents().getValues();
            if (comps != null && !comps.isEmpty() && comps.get(0) != null) {
                String json = comps.get(0).getJson();
                if (json != null && !json.isEmpty()) {
                    return GsonComponentSerializer.gson().deserialize(json);
                }
            }
        } catch (Throwable ignored) { }

        try {
            // Some packets may store a plain string parameter at index 0
            String plain = packet.getStrings().readSafely(0);
            if (plain != null) {
                return Component.text(plain);
            }
        } catch (Throwable ignored) { }

        return null;
    }
}
