package net.bteuk.network.proxy;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.entity.Role;
import net.bteuk.network.core.Constants;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.network.lib.dto.TabPlayer;
import org.btuk.network.lib.utils.ChatUtils;
import org.btuk.proxy.core.chat.ChatHandler;
import org.btuk.proxy.core.config.Config;
import org.btuk.proxy.core.player.Player;
import org.btuk.proxy.core.scheduler.Scheduler;
import org.btuk.proxy.core.tab.AbstractTabManager;
import org.btuk.proxy.core.user.CoreUserManager;
import org.btuk.proxy.core.user.User;
import org.bukkit.Server;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Log
public class NetworkTabManager extends AbstractTabManager {

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    private final Network plugin;
    private final Server server;
    private final Roles roles;
    private final Constants constants;

    private final Map<UUID, Scoreboard> scoreboards;
    private final Map<String, String> sortKeys;

    public NetworkTabManager(Network plugin, Server server, Roles roles, Constants constants, Config config, CoreUserManager coreUserManager, ChatHandler chatHandler, Scheduler scheduler) {
        super(config, coreUserManager, chatHandler, scheduler);
        this.plugin = plugin;
        this.server = server;
        this.roles = roles;
        this.constants = constants;

        this.scoreboards = new HashMap<>();
        this.sortKeys = new HashMap<>();

        initSortKeys();
    }

    private void initSortKeys() {
        int i = 0;
        int j = 0;
        for (Role role : roles.getRoles()) {
            sortKeys.put(role.getId(), String.valueOf(ALPHABET[i]) + ALPHABET[j]);
            if (j == 25) {
                i++;
                j = 0;
            } else {
                j++;
            }
        }
    }

    @Override
    protected void addPlayerToTabList(Player player, User user, TabPlayer tabPlayer) {
        server.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(tabPlayer.getUuid());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
                return;
            }

            updateBukkitPlayerListName(bukkitPlayer, tabPlayer);

            for (org.bukkit.entity.Player onlinePlayer : server.getOnlinePlayers()) {
                // Update the scoreboard for the online player.
                User viewerUser = coreUserManager.getUserByUuid(onlinePlayer.getUniqueId().toString());
                if (viewerUser != null) {
                    updatePlayerInScoreboard(getScoreboard(onlinePlayer), tabPlayer);
                }
                onlinePlayer.listPlayer(bukkitPlayer);
            }
        });
    }

    @Override
    protected void removePlayerFromTabList(Player player, TabPlayer tabPlayer) {
        // Remove the scoreboard if the player leaving is the owner of the scoreboard.
        scoreboards.remove(UUID.fromString(tabPlayer.getUuid()));

        for (org.bukkit.entity.Player onlinePlayer : server.getOnlinePlayers()) {
            Scoreboard sb = scoreboards.get(onlinePlayer.getUniqueId());
            if (sb != null) {
                Team team = sb.getEntryTeam(tabPlayer.getName());
                if (team != null) {
                    team.unregister();
                }
            }
        }
    }

    @Override
    protected void updatePlayerPing(String name, int ping) {
        // Do nothing in standalone mode ping real players are shown in tab, their ping is already correct.
    }

    @Override
    protected void updatePlayerDisplayName(String name, TabPlayer updated) {
        server.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(updated.getUuid());
            if (bukkitPlayer != null && bukkitPlayer.isOnline()) {
                updateBukkitPlayerListName(bukkitPlayer, updated);
            }

            // Update the display name in all scoreboards.
            for (org.bukkit.entity.Player onlinePlayer : server.getOnlinePlayers()) {
                User viewerUser = coreUserManager.getUserByUuid(onlinePlayer.getUniqueId().toString());
                if (viewerUser != null) {
                    updatePlayerInScoreboard(getScoreboard(onlinePlayer), updated);
                }
            }
        });
    }

    @Override
    protected int findPingForPlayer(String uuid) {
        org.bukkit.entity.Player player = resolveBukkitPlayer(uuid);
        return player != null ? player.getPing() : -1;
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
        server.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(user.getUuid());
            if (bukkitPlayer != null) {
                TabPlayer tabPlayerToUpdate = findTabPlayerByUuid(userToUpdate.getUuid());
                if (tabPlayerToUpdate != null) {
                    org.bukkit.entity.Player targetPlayer = resolveBukkitPlayer(tabPlayerToUpdate.getUuid());
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        updateBukkitPlayerListName(targetPlayer, tabPlayerToUpdate);
                    }
                    updatePlayerInScoreboard(getScoreboard(bukkitPlayer), tabPlayerToUpdate);
                }
            }
        });
    }

    /**
     * Send the full tablist to a user.
     * This is used when a user connects to a server.
     * Adjust display names for muted players.
     */
    @Override
    public void sendTablist(User user) {
        server.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player player = resolveBukkitPlayer(user.getUuid());
            if (player == null || !player.isOnline()) {
                return;
            }

            // Set the scoreboard for the player.
            player.setScoreboard(getScoreboard(player));

            // Send header and footer.
            player.sendPlayerListHeaderAndFooter(HEADER, FOOTER);

            for (org.bukkit.entity.Player onlinePlayer : server.getOnlinePlayers()) {
                // Add the other player to this player's scoreboard.
                TabPlayer tabPlayer = findTabPlayerByUuid(onlinePlayer.getUniqueId().toString());
                if (tabPlayer != null) {
                    updateBukkitPlayerListName(onlinePlayer, tabPlayer);
                    updatePlayerInScoreboard(player.getScoreboard(), tabPlayer);
                }
                player.listPlayer(onlinePlayer);
            }
        });
    }

    private void updatePlayerInScoreboard(Scoreboard sb, TabPlayer target) {
        String teamName = getTeamName(target);
        Team team = sb.getTeam(teamName);

        // If the player was in a different team, remove them first.
        Team oldTeam = sb.getEntryTeam(target.getName());
        if (oldTeam != null && !oldTeam.getName().equals(teamName)) {
            oldTeam.removeEntry(target.getName());
        }

        if (team == null) {
            team = sb.registerNewTeam(teamName);
        }

        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    private void updateBukkitPlayerListName(org.bukkit.entity.Player bukkitPlayer, TabPlayer tabPlayer) {
        bukkitPlayer.playerListName(globalDisplayName(tabPlayer));
    }

    private Component globalDisplayName(TabPlayer tabPlayer) {
        User userToAdd = coreUserManager.getUserByUuid(tabPlayer.getUuid());

        Style.Builder statusStyle = Style.style();

        Component name = ChatUtils.line(tabPlayer.getName());
        if (userToAdd != null) {
            if (userToAdd.isMuted()) {
                statusStyle.color(NamedTextColor.DARK_RED);
            }
            if (userToAdd.isAfk()) {
                statusStyle.decorate(TextDecoration.ITALIC);
                statusStyle.color(NamedTextColor.WHITE);
            }
            if (userToAdd.isFocusEnabled()) {
                statusStyle.decorate(TextDecoration.STRIKETHROUGH);
                statusStyle.color(NamedTextColor.WHITE);
            }
            name = userToAdd.getDisplayName();
        }

        Style style = statusStyle.build();
        if (!style.isEmpty()) {
            name = applyStyleToTree(name, style);
        }

        Component prefix = Component.empty();

        if (tabPlayer.getPrefix() != null) {
            prefix = prefix.append(tabPlayer.getPrefix()).append(Component.space());
        }

        name = prefix.append(name);

        return name;
    }

    private String getTeamName(TabPlayer tabPlayer) {
        String sortKey = sortKeys.getOrDefault(tabPlayer.getPrimaryGroup(), "zz");
        return sortKey + tabPlayer.getUuid().substring(0, 14);
    }

    private Scoreboard getScoreboard(org.bukkit.entity.Player player) {
        return scoreboards.computeIfAbsent(player.getUniqueId(), _ -> {
            Scoreboard sb = server.getScoreboardManager().getNewScoreboard();

            // Set sidebar if enabled.
            if (constants.sidebarEnabled()) {
                Objective objective = sb.registerNewObjective("sidebar", Criteria.DUMMY,
                        Component.text(constants.sidebarTitle()));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                int score = constants.sidebarContent().size();
                for (String sidebarText : constants.sidebarContent()) {
                    score--;
                    objective.getScore(sidebarText).setScore(score);
                }
            }
            return sb;
        });
    }

    private @Nullable org.bukkit.entity.Player resolveBukkitPlayer(String uuid) {
        return server.getOnlinePlayers().stream().filter(player -> player.getUniqueId().toString().equals(uuid)).findFirst().orElse(null);
    }

    private static Component applyStyleToTree(Component component, Style styleToApply) {
        Component updated = component.style(styleToApply);

        if (!component.children().isEmpty()) {
            List<Component> updatedChildren = component.children().stream()
                    .map(child -> applyStyleToTree(child, styleToApply))
                    .toList();
            updated = updated.children(updatedChildren);
        }

        return updated;
    }
}
