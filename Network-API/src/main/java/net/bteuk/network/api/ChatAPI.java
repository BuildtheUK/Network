package net.bteuk.network.api;

import org.btuk.network.lib.dto.ChatMessage;
import org.btuk.network.lib.dto.DirectMessage;
import org.btuk.network.lib.dto.DiscordDirectMessage;
import org.btuk.network.lib.dto.PlotMessage;

public interface ChatAPI {

    void sendChatMessage(ChatMessage chatMessage);

    void sendDirectMessage(DirectMessage directMessage);

    void sendPlotMessage(PlotMessage plotMessage);

    void sendDiscordDirectMessage(DiscordDirectMessage discordDirectMessage);

}
