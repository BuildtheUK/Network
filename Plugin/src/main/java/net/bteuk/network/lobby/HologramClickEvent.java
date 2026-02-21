package net.bteuk.network.lobby;

import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class HologramClickEvent implements Listener {

    private final Network instance;

    private final Map map;

    public HologramClickEvent(Network instance, Map map) {
        this.instance = instance;
        this.map = map;

        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    public void unregister() {
        eu.decentsoftware.holograms.event.HologramClickEvent.getHandlerList().unregister(this);
    }

    @EventHandler
    public void onHologramClick(eu.decentsoftware.holograms.event.HologramClickEvent e) {

        // Get the user.
        NetworkUser u = instance.getUser(e.getPlayer());
        if (u == null) {
            e.getPlayer().sendMessage(ChatUtils.error("An error occurred, please rejoin."));
            return;
        }

        // Get the hologram click event.
        Map.HologramClickAction action = map.getHologramClickAction(e.getHologram());

        // Handle click event on hologram.
        if (action != null) {
            action.click(u);
        }
    }
}
