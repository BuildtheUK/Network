package net.bteuk.network.papercore;

import net.bteuk.network.api.entity.NetworkPlayer;
import org.bukkit.entity.Player;

public class PlayerAdapter {

    public static NetworkPlayer adapt(Player player) {
        return new NetworkPlayerImpl(player);
    }
}
