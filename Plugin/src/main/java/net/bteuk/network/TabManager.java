package net.bteuk.network;

import lombok.Getter;
import lombok.extern.java.Log;
import net.bteuk.network.api.entity.Role;
import net.bteuk.network.core.Constants;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import org.btuk.network.lib.dto.AddTeamEvent;
import org.btuk.network.lib.dto.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Log
public class TabManager {

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private final Network instance;
    private final Constants constants;
    private final Roles roles;
    private final Map<String, Team> teams = new HashMap<>();
    @Getter
    private Scoreboard scoreboard;

    public TabManager(Network instance, Constants constants, Roles roles) {

        this.instance = instance;
        this.constants = constants;
        this.roles = roles;

        // Teams are used to sort the tab-list by role.
        initTeams();
    }

    public void hidePlayerInTabList(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.unlistPlayer(player);
        }
    }

    public void hidePlayersFromTabList(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.unlistPlayer(onlinePlayer);
        }
    }

    public TabPlayer createTabPlayerFromPlayer(Player player) {
        TabPlayer tabPlayer = new TabPlayer();
        tabPlayer.setUuid(player.getUniqueId().toString());
        tabPlayer.setName(player.getName());
        tabPlayer.setPing(player.getPing());
        Role primaryRole = roles.getPrimaryRole(player);

        if (primaryRole != null) {
            tabPlayer.setPrimaryGroup(primaryRole.getId());
            tabPlayer.setPrefix(primaryRole.getColouredPrefix());
        }

        return tabPlayer;
    }

    /**
     * Handler for an {@link AddTeamEvent}
     *
     * @param addTeamEvent the event
     */
    public void handle(AddTeamEvent addTeamEvent) {
        addToTeam(addTeamEvent.getName(), addTeamEvent.getPrimaryGroup());
    }

    public void onPlayerJoin(Player player) {
        if (!instance.isStandalone()) {
            player.setScoreboard(scoreboard);
        }
    }

    /**
     * Initializes the team for the tab-list sorting by role.
     */
    private void initTeams() {

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        // Get all the roles.
        Set<Role> roles = this.roles.getRoles();

        // Each role will get a 2 character name in order of the alphabet to ensure correct sorting.
        // This has the limitation of 26^2 number of possible roles, but if you go over that you're a bit crazy.
        int i = 0;
        int j = 0;

        for (Role role : roles) {
            // Create a team for this role and add it to the hashmap.
            teams.put(role.getId(), createTeam(role, String.valueOf(ALPHABET[i]) + ALPHABET[j]));

            // Increase the counters.
            if (j == 25) {
                i++;
                j = 0;
            } else {
                j++;
            }
        }

        // Set sidebar if enabled.
        if (constants.sidebarEnabled()) {
            Objective objective = scoreboard.registerNewObjective("sidebar", Criteria.DUMMY,
                    Component.text(constants.sidebarTitle()));
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            int score = constants.sidebarContent().size();
            for (String sidebarText : constants.sidebarContent()) {
                score--;
                objective.getScore(sidebarText).setScore(score);
            }
        }
    }

    private Team createTeam(Role role, String name) {

        Team team = scoreboard.getTeam(name);

        if (team != null) {
            // Remove all players from the team.
            team.removeEntries(team.getEntries());
        } else {
            // Create team.
            team = scoreboard.registerNewTeam(name);
        }

        // Set the team prefix.
        team.prefix(role.getColouredPrefix().append(Component.space()));
        return team;
    }

    /**
     * Adds a player to a team for tab-list sorting by role
     * .
     *
     * @param name        the name of the player to add to the team
     * @param primaryRole the role of the player
     */
    public void addToTeam(String name, String primaryRole) {

        // Run this task synchronously.
        instance.getServer().getScheduler().runTask(instance, () -> {
            // Get the team based on the primaryRole.
            Team team = teams.get(primaryRole);

            if (team != null) {
                team.addEntry(name);
            } else {
                log.warning(String.format("Player %s with primary role %s does not have a team.", name,
                        primaryRole));
            }
        });
    }
}
