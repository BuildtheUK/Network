package net.bteuk.network.papercore;

import net.bteuk.network.api.entity.NetworkPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class NetworkPlayerImpl implements NetworkPlayer {

    private final Player player;

    public NetworkPlayerImpl(Player player) {
        this.player = player;
    }

    @Override
    public String getUuidAsString() {
        return player.getUniqueId().toString();
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public void sendMessage(Component message) {
        player.sendMessage(message);
    }
}
