package net.bteuk.network;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import lombok.extern.java.Log;
import net.bteuk.network.api.entity.Role;
import net.bteuk.network.api.entity.ShutdownHook;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.dto.AddTeamEvent;
import net.bteuk.network.lib.dto.TabPlayer;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction.ADD_PLAYER;

@Log
public class TabManager implements ShutdownHook {

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private final Network instance;
    private final Constants constants;
    private final Roles roles;
    private final ProtocolManager pm;
    private final Map<String, Team> teams = new HashMap<>();
    private PacketListener pl;
    private Scoreboard scoreboard;

    public TabManager(Network instance, Constants constants, Roles roles) {

        this.instance = instance;
        this.constants = constants;
        this.roles = roles;
        pm = ProtocolLibrary.getProtocolManager();

        // Teams are used to sort the tab-list by role.
        initTeams();

        instance.registerShutdownHook(this);

        startTab();
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
        player.setScoreboard(scoreboard);
    }

    @Override
    public void shutdown() {
        pm.removePacketListener(pl);
        log.info("Disabled Tab");
    }

    private void startTab() {
        createPacketListeners();
        pm.addPacketListener(pl);
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

    /**
     * Creates a packet listener to set listed = false for all real players.
     * The visual tab-list is only comprised of fake players,
     * this makes it easier to have the same tab-lists between servers and to customise display per player.
     * <p>
     * Real players can't be stopped altogether since they are required for the above-head name and chat session.
     */
    private void createPacketListeners() {
        pl = new PacketAdapter(instance, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                if (packet.getPlayerInfoActions().read(0).contains(ADD_PLAYER)) {

                    List<PlayerInfoData> infoList = packet.getPlayerInfoDataLists().read(1);
                    List<PlayerInfoData> newInfoList = new ArrayList<>();

                    infoList.forEach(info -> {
                        // Create an exact copy, but set 'listed' to false.
                        newInfoList.add(new PlayerInfoData(info.getProfileId(), info.getLatency(), false,
                                info.getGameMode(), info.getProfile(), info.getDisplayName(),
                                info.getRemoteChatSessionData()));
                    });

                    packet.getPlayerInfoDataLists().write(1, newInfoList);
                    event.setPacket(packet);
                }
            }
        };
    }
}
