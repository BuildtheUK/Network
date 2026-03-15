package net.bteuk.network.socket;

import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.socket.OutputSocket;
import net.bteuk.network.lib.socket.SocketHandler;

public class StandaloneOutputSocket extends OutputSocket {

    private final SocketHandler socketHandler;

    public StandaloneOutputSocket(SocketHandler socketHandler) {
        super(null, 0);
        this.socketHandler = socketHandler;
    }

    @Override
    public boolean sendSocketMessage(AbstractTransferObject transferObject) {
        socketHandler.handle(transferObject);
        return true;
    }
}
