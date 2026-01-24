package net.bteuk.network;

import lombok.extern.java.Log;
import net.bteuk.network.core.Constants;
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
import net.bteuk.network.lib.socket.InputSocket;
import net.bteuk.network.lib.socket.OutputSocket;
import net.bteuk.network.lib.socket.SocketHandler;

@Log
public class SocketHandlerImpl implements SocketHandler {

    private final Network instance;

    private CustomChat chat;

    private TabManager tabManager;

    private Connect connect;

    private OutputSocket outputSocket;
    private InputSocket inputSocket;

    private static SocketHandlerImpl socketHandler;

    /**
     *
     * @param message
     * @return Whether the socket message was sent
     */
    public static boolean sendSocketMessageIfOnline(AbstractTransferObject message)
    {
        if (socketHandler != null) {
            socketHandler.sendSocketMessage(message);
            return true;
        }
        else
            return false;
    }

    public SocketHandlerImpl(Network instance, CustomChat chat, TabManager tabManager, Connect connect, Constants constants) {
        this.instance = instance;
        this.chat = chat;
        this.tabManager = tabManager;
        this.connect = connect;

        // Set up the output socket.
        outputSocket = new OutputSocket(constants.chatSocketOutputIP(), constants.chatSocketOutputPort());

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

        socketHandler = this;
    }

    public SocketHandlerImpl(Network instance, Constants constants) {
        this.instance = instance;

        // Set up the output socket.
        outputSocket = new OutputSocket(constants.chatSocketOutputIP(), constants.chatSocketOutputPort());

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

        socketHandler = this;
    }

    public void addComponents(CustomChat chat, TabManager tabManager, Connect connect) {
        this.chat = chat;
        this.tabManager = tabManager;
        this.connect = connect;
    }

    public void sendSocketMessage(AbstractTransferObject message) {
        outputSocket.sendSocketMessage(message);
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

    public void registerSocketHandler(SocketHandler socketHandler) {
        inputSocket.start(socketHandler);
    }
}
