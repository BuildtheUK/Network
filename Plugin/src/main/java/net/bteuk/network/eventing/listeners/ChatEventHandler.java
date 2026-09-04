package net.bteuk.network.eventing.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;

@FunctionalInterface
public interface ChatEventHandler {

    /**
     * Handle the chat event, return true if the event was handled and the listener should be unregistered.
     *
     * @param event the chat event
     * @return true if the event was handled and the listener should be unregistered
     */
    boolean handleChatEvent(AsyncChatEvent event);

}
