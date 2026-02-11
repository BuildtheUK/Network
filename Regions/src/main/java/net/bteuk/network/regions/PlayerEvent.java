package net.bteuk.network.regions;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface PlayerEvent {
    void playerEvent(Player player);
}
