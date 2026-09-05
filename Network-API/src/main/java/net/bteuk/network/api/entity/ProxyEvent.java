package net.bteuk.network.api.entity;

import org.btuk.network.lib.dto.AbstractTransferObject;

/**
 * Abstract class for events sent by the proxy.
 */
public interface ProxyEvent<T extends AbstractTransferObject> {

    void event(T event);
}
