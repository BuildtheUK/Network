package net.bteuk.network.proxy;

import org.btuk.network.lib.dto.TabPlayer;
import org.btuk.proxy.core.chat.ChatHandler;
import org.btuk.proxy.core.config.Config;
import org.btuk.proxy.core.player.Player;
import org.btuk.proxy.core.scheduler.Scheduler;
import org.btuk.proxy.core.tab.AbstractTabManager;
import org.btuk.proxy.core.user.CoreUserManager;
import org.btuk.proxy.core.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;

public class NetworkTabManager extends AbstractTabManager {

    private final Server server;

    public NetworkTabManager(Server server, Config config, CoreUserManager coreUserManager, ChatHandler chatHandler, Scheduler scheduler) {
        super(config, coreUserManager, chatHandler, scheduler);
        this.server = server;
    }

    @Override
    protected void addPlayerToTabList(Player player, User user, TabPlayer tabPlayer) {
        org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(player.getUniqueId().toString());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }
        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.listPlayer(bukkitPlayer);
        }
    }

    @Override
    protected void removePlayerFromTabList(Player player, TabPlayer tabPlayer) {
        // Do nothing in standalone mode ping real players are shown in tab; when they disconnect they should automatically be removed.

    }

    @Override
    protected void updatePlayerPing(String name, int ping) {
        // Do nothing in standalone mode ping real players are shown in tab, their ping is already correct.
    }

    @Override
    protected void updatePlayerDisplayName(String name, TabPlayer updated) {
        // Do nothing, the display name of the real player should already be correct.
        // TODO: Ensure stuff like mute, afk, focus work. Pmute is not possible as displaynames are global.
    }

    @Override
    protected int findPingForPlayer(String uuid) {
        return -1;
    }

    @Override
    protected void updatePing() {
        // Do nothing in standalone mode ping real players are shown in tab, their ping is already correct.
    }

    /**
     * Update a specific user in the tablist of another user.
     * This can be used specifically when you do a personal mute of a player.
     *
     * @param user         the user to update the tablist for
     * @param userToUpdate the user to update in the tablist
     */
    @Override
    public void updatePlayerInTablistOfPlayer(User user, User userToUpdate) {
        // Do nothing in standalone mode ping real players are shown in tab.
    }

    /**
     * Send the full tablist to a user.
     * This is used when a user connects to a server.
     * Adjust display names for muted players.
     */
    @Override
    public void sendTablist(User user) {
        org.bukkit.entity.Player player = resolveBukkitPlayer(user.getUuid());
        if (player == null || !player.isOnline()) {
            return;
        }
        for (org.bukkit.entity.Player onlinePlayer : server.getOnlinePlayers()) {
            player.listPlayer(onlinePlayer);
        }
    }

    private @Nullable org.bukkit.entity.Player resolveBukkitPlayer(String uuid) {
        return server.getOnlinePlayers().stream().filter(player -> player.getUniqueId().toString().equals(uuid)).findFirst().orElse(null);
    }
}
