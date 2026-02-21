package net.bteuk.network.api.entity;

import net.kyori.adventure.text.Component;

public interface NetworkPlayer {

    String getUuidAsString();

    String getName();

    void sendMessage(Component message);

}
