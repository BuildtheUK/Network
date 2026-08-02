package net.bteuk.network.socket;

import lombok.extern.java.Log;
import net.bteuk.network.core.Constants;
import org.btuk.network.lib.dto.AbstractTransferObject;
import org.btuk.network.lib.socket.OutputSocket;
import org.btuk.proxy.core.socket.ProxySocketHandler;

import java.util.function.Consumer;

/**
 * Sends messages to the proxy.
 */
@Log
public class MessageSender {

    private final boolean isStandalone;

    private OutputSocket outputSocket;

    public MessageSender(Constants constants) {
        this.isStandalone = constants.standalone();
        if (!isStandalone) {
            outputSocket = new OutputSocket(constants.chatSocketOutputIP(), constants.chatSocketOutputPort());
        }
    }

    public void sendSocketMessage(AbstractTransferObject message) {
        if (outputSocket != null) {
            outputSocket.sendSocketMessage(message);
        } else {
            log.severe("Message sent while output socket is not yet initialised.");
        }
    }

    public Consumer<ProxySocketHandler> setupStandaloneOutputSocket() {
        if (isStandalone) {
            return socketHandler -> outputSocket = new StandaloneOutputSocket(socketHandler);
        }
        return null;
    }
}
