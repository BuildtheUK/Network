package net.bteuk.network.proxy;

import net.kyori.adventure.text.Component;
import org.btuk.proxy.core.player.Player;
import org.btuk.proxy.core.server.Server;

import java.util.UUID;

public class NetworkPlayer implements Player {

    private final org.bukkit.entity.Player bukkitPlayer;

    public NetworkPlayer(org.bukkit.entity.Player bukkitPlayer) {
        this.bukkitPlayer = bukkitPlayer;
    }

    @Override
    public void connectToServer(Server server) {
        // Do nothing, this is only used for standalone servers.
    }

    @Override
    public UUID getUniqueId() {
        return bukkitPlayer.getUniqueId();
    }

    @Override
    public boolean hasPermission(String permission) {
        return bukkitPlayer.hasPermission(permission);
    }

    @Override
    public String getUsername() {
        return bukkitPlayer.getName();
    }

    @Override
    public void sendPlayerListHeaderAndFooter(Component header, Component footer) {
        bukkitPlayer.sendPlayerListHeaderAndFooter(header, footer);
    }
}
