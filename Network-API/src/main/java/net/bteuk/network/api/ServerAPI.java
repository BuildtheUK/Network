package net.bteuk.network.api;

import net.bteuk.network.api.entity.NetworkPlayer;

public interface ServerAPI {

    void switchServer(NetworkPlayer player, String server);

}
