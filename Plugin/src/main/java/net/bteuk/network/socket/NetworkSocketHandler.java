package net.bteuk.network.socket;

import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.TabManager;
import net.bteuk.network.commands.navigation.Teleport;
import net.bteuk.network.core.Constants;
import net.bteuk.network.eventing.listeners.Connect;
import org.btuk.network.lib.dto.AbstractTransferObject;
import org.btuk.network.lib.dto.AddTeamEvent;
import org.btuk.network.lib.dto.DirectMessage;
import org.btuk.network.lib.dto.DiscordLinking;
import org.btuk.network.lib.dto.OnlineUserAdd;
import org.btuk.network.lib.dto.OnlineUserRemove;
import org.btuk.network.lib.dto.OnlineUsersReply;
import org.btuk.network.lib.dto.ProxyStart;
import org.btuk.network.lib.dto.TeleportEvent;
import org.btuk.network.lib.dto.UserConnectReply;
import org.btuk.network.lib.dto.UserRemove;
import org.btuk.network.lib.dto.UserUpdate;
import org.btuk.network.lib.socket.InputSocket;
import org.btuk.network.lib.socket.SocketHandler;

@Log
public class NetworkSocketHandler implements SocketHandler {

    private final Network instance;

    private final CustomChat chat;

    private final TabManager tabManager;

    private final Connect connect;

    private InputSocket inputSocket;

    private final Teleport teleport;

    public NetworkSocketHandler(Network instance, CustomChat chat, TabManager tabManager, Connect connect, Constants constants, Teleport teleport) {
        this.instance = instance;
        this.chat = chat;
        this.tabManager = tabManager;
        this.connect = connect;
        this.teleport = teleport;

        // Register input socket for receiving messages from the proxy.
        int inputSocketPort = constants.chatSocketInputPort();
        if (inputSocketPort == 0) {
            log.severe("Input socket port is not set in config or is set to 0. Please set a valid port!");
        } else {
            // Create the input socket.
            inputSocket = new InputSocket(inputSocketPort);
        }

        // Register the socket handler.
        registerSocketHandler(this);
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
            case TeleportEvent teleportEvent -> teleport.handleTeleportEvent(teleportEvent);
            case ProxyStart proxyStart -> instance.handleProxyStart(proxyStart);
            default -> log.warning(String.format("Socket object has an unrecognised type %s",
                    abstractTransferObject.getClass().getTypeName()));
        }
        return null;
    }

    public void registerSocketHandler(SocketHandler socketHandler) {
        inputSocket.start(socketHandler);
    }
}
