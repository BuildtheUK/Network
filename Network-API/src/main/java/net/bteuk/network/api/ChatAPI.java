package net.bteuk.network.api;

import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.DiscordDirectMessage;
import net.bteuk.network.lib.dto.PlotMessage;

public interface ChatAPI {

    void sendChatMessage(ChatMessage chatMessage);

    void sendDirectMessage(DirectMessage directMessage);

    void sendPlotMessage(PlotMessage plotMessage);

    void sendDiscordDirectMessage(DiscordDirectMessage discordDirectMessage);

}
