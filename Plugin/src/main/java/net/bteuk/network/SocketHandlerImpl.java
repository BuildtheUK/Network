package net.bteuk.network;

import lombok.extern.java.Log;
import net.bteuk.network.eventing.listeners.Connect;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.dto.AddTeamEvent;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.OnlineUserAdd;
import net.bteuk.network.lib.dto.OnlineUserRemove;
import net.bteuk.network.lib.dto.OnlineUsersReply;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserRemove;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.socket.SocketHandler;

@Log
public class SocketHandlerImpl implements SocketHandler {

    private final Network instance;

    private final CustomChat chat;

    private final TabManager tabManager;

    private final Connect connect;

    public SocketHandlerImpl(Network instance, CustomChat chat, TabManager tabManager, Connect connect) {
        this.instance = instance;
        this.chat = chat;
        this.tabManager = tabManager;
        this.connect = connect;

        // Register the socket handler.
        chat.registerSocketHandler(this);
    }

    @Override
    public AbstractTransferObject handle(AbstractTransferObject abstractTransferObject) {
        switch (abstractTransferObject) {
            case DirectMessage directMessage -> chat.handleDirectMessage(directMessage);
            case DiscordLinking discordLinking -> chat.handleDiscordLinking(discordLinking);
            case AddTeamEvent addTeamEvent -> tabManager.handle(addTeamEvent);
            case UserConnectReply userConnectReply -> connect.handleUserConnectReply(userConnectReply);
            case UserRemove userRemove -> connect.handleUserRemove(userRemove);
            case UserUpdate userUpdate -> chat.handleUserUpdate(userUpdate);
            case OnlineUsersReply onlineUsersReply -> instance.handleOnlineUsersReply(onlineUsersReply);
            case OnlineUserAdd onlineUserAdd -> instance.handleOnlineUserAdd(onlineUserAdd);
            case OnlineUserRemove onlineUserRemove -> instance.handleOnlineUserRemove(onlineUserRemove);
            default -> log.warning(String.format("Socket object has an unrecognised type %s",
                    abstractTransferObject.getClass().getTypeName()));
        }
        return null;
    }
}
