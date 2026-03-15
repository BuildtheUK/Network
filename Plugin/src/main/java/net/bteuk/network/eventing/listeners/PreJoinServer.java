package net.bteuk.network.eventing.listeners;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.core.Constants;
import net.bteuk.network.exceptions.NotBannedException;
import net.bteuk.network.utils.staff.Moderation;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

@Log
public class PreJoinServer implements Listener {

    private final Network instance;
    private final Constants constants;
    private final Moderation moderation;

    public PreJoinServer(Network instance, Constants constants, Moderation moderation) {
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
        this.instance = instance;
        this.constants = constants;
        this.moderation = moderation;
    }

    @EventHandler
    public void preJoin(AsyncPlayerPreLoginEvent e) {
        // If the player is banned, stop them from logging in.
        if (constants.moderationEnabled() && moderation.isBanned(e.getUniqueId().toString())) {
            try {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, moderation.getBannedComponent(e.getUniqueId().toString()));
            } catch (NotBannedException ex) {
                log.severe("The player is no longer banned, but they were less than a millisecond ago!");
            }
        }

        // Check if server is restarting.
        if (instance.allowShutdown) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("The server is restarting!"));
        }
    }
}
