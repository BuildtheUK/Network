package net.bteuk.network.proxy;

import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.socket.NetworkSocketHandler;
import org.btuk.proxy.core.chat.ChatHandler;

public class NetworkChatHandler implements ChatHandler {

    private final NetworkSocketHandler socketHandler;

    public NetworkChatHandler(NetworkSocketHandler socketHandler) {
        this.socketHandler = socketHandler;
    }

    @Override
    public void handle(AbstractTransferObject abstractTransferObject) {
        socketHandler.handle(abstractTransferObject);
    }

    @Override
    public void handle(AbstractTransferObject abstractTransferObject, String server) {
        socketHandler.handle(abstractTransferObject);
    }
}
