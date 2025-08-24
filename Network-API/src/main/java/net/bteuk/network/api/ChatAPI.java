package net.bteuk.network.api;

import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;

public interface ChatAPI {

    void sendChatMessage(ChatMessage chatMessage);

    void sendDirectMessage(DirectMessage directMessage);

}
