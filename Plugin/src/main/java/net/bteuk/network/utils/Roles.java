package net.bteuk.network.utils;

import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.api.RoleAPI;
import net.bteuk.network.api.entity.Role;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.network.lib.dto.TabPlayer;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.PlotSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;

@Log
public final class Roles implements RoleAPI {

    private static final Component PROMOTION_TEMPLATE = Component.text(" has been promoted to ");
    private static final Component PROMOTION_SELF = Component.text("You have been promoted to ");
    private static final LinkedHashSet<String> BUILDER_ROLE_NAMES = Stream.of("reviewer", "architect", "builder",
                    "jrbuilder", "apprentice", "applicant", "default")
            .collect(Collectors.toCollection(LinkedHashSet::new));
    private static Set<Role> ROLES;

    private final Network instance;
    private final PlotSQL plotSQL;
    private CustomChat customChat;

    public Roles(Network instance, PlotSQL plotSQL) {
        this.instance = instance;
        this.plotSQL = plotSQL;
    }

    public void registerChat(CustomChat customChat) {
        if (this.customChat == null) {
            this.customChat = customChat;
        }
    }

    @Override
    public Set<Role> getRoles() {
        if (ROLES == null) {
            loadRoles();
        }
        return ROLES;
    }

    public Role getRoleById(String roleId) {
        // Get the configuration if not yet fetches.
        if (ROLES == null) {
            loadRoles();
        }
        return ROLES.stream().filter(role -> role.getId().equalsIgnoreCase(roleId)).findFirst().orElse(null);
    }

    /*

        Get the builder role of the player.

        Builder roles include:
            Default (Guest)
            Applicant
            Apprentice
            Jr.Builder
            Builder
            Architect

     */
    public Role builderRole(Player p) {
        String roleToGet = "default";
        for (String roleName : BUILDER_ROLE_NAMES) {
            if (p.hasPermission("group." + roleName)) {
                roleToGet = roleName;
                break;
            }
        }
        return getRoleById(roleToGet);
    }

    /**
     * Get the builder role for a potentially offline player.
     *
     * @param uuid the uuid of the player
     * @return a {@link CompletableFuture} with a String
     */
    @Override
    public CompletableFuture<String> getBuilderRole(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            CompletableFuture<Boolean> isReviewer = Permissions.hasGroup(uuid, "reviewer");
            CompletableFuture<Boolean> isArchitect = Permissions.hasGroup(uuid, "architect");
            CompletableFuture<Boolean> isBuilder = Permissions.hasGroup(uuid, "builder");
            CompletableFuture<Boolean> isJrbuilder = Permissions.hasGroup(uuid, "jrbuilder");
            CompletableFuture<Boolean> isApprentice = Permissions.hasGroup(uuid, "apprentice");
            CompletableFuture<Boolean> isApplicant = Permissions.hasGroup(uuid, "applicant");
            if (isReviewer != null && isReviewer.join()) {
                return "reviewer";
            } else if (isArchitect != null && isArchitect.join()) {
                return "architect";
            } else if (isBuilder != null && isBuilder.join()) {
                return "builder";
            } else if (isJrbuilder != null && isJrbuilder.join()) {
                return "jrbuilder";
            } else if (isApprentice != null && isApprentice.join()) {
                return "apprentice";
            } else if (isApplicant != null && isApplicant.join()) {
                return "applicant";
            } else {
                return "default";
            }
        });
    }

    public Role getPrimaryRole(Player p) {
        // Get the configuration if not yet fetches.
        if (ROLES == null) {
            loadRoles();
        }
        for (Role role : ROLES) {
            if (p.hasPermission(String.format("group.%s", role.getId()))) {
                return role;
            }
        }
        return null;
    }

    private void loadRoles() {
        // Create roles.yml if not exists.
        // The data folder should already exist since the plugin will always create config.yml first.
        File rolesFile = new File(instance.getDataFolder(), "roles.yml");
        if (!rolesFile.exists()) {
            instance.saveResource("roles.yml", false);
        }

        FileConfiguration rolesConfig = YamlConfiguration.loadConfiguration(rolesFile);

        // Gets all the roles from the config.
        ConfigurationSection roles = rolesConfig.getConfigurationSection("roles");

        if (roles == null) {
            return;
        }

        Set<String> keys = roles.getKeys(false);

        ROLES = new TreeSet<>();
        // Add the roles.
        keys.forEach(key -> ROLES.add(new Role(
                key,
                roles.getString(key + ".name", null),
                roles.getString(key + ".prefix", null),
                roles.getString(key + ".colour", null),
                roles.getInt(key + ".weight", 0)))
        );
    }

    /**
     * Promote/demote a player for a specific role.
     *
     * @param uuid     the uuid of the player to promote.
     * @param roleId   the role to add or remove
     * @param remove   whether to remove the role or not
     * @param announce whether to announce the promotion (demotion is never announced)
     * @return {@link CompletableFuture} completableFuture with {@link Component} message.
     */
    public CompletableFuture<Component> alterRole(String uuid, String name, String roleId, boolean remove,
                                                         boolean announce) {

        if (customChat == null) {
            throw new IllegalStateException("CustomChat is not initialized.");
        }

        // Get the configured group.
        Role role = getRoleById(roleId);
        Group group = Permissions.getGroup(roleId);

        if (group == null || role == null) {
            return CompletableFuture.completedFuture(ChatUtils.error("%s is not configured in LuckPerms and/or roles" +
                    ".yml.", roleId));
        }

        return CompletableFuture.supplyAsync(() -> {

            String groupBefore;
            try {
                groupBefore = Objects.requireNonNull(Permissions.getPrimaryGroup(uuid)).join();
            } catch (Exception e) {
                return ChatUtils.error("An error occurred while fetching the primary group.");
            }

            String groupAfter = Permissions.modifyGroup(uuid, group, remove);

            if (groupAfter == null) {
                return ChatUtils.error("Modifying the permissions failed!");
            }

            log.info(String.format("Group before %s, group after %s", groupBefore, groupAfter));
            if (!groupBefore.equals(groupAfter)) {
                // Update primary role in TAB.
                Role primaryRole = getRoleById(groupAfter);
                TabPlayer tabPlayer = new TabPlayer();
                tabPlayer.setUuid(uuid);
                tabPlayer.setName(name);
                tabPlayer.setPrimaryGroup(primaryRole.getId());
                tabPlayer.setPrefix(primaryRole.getColouredPrefix());
                UserUpdate userUpdate = new UserUpdate();
                userUpdate.setUuid(uuid);
                userUpdate.setTabPlayer(tabPlayer);
                customChat.sendSocketMessage(userUpdate);

                // If the new primary role is architect or reviewer, and they were promoted add them to the reviewers
                // database table.
                if (!remove && (primaryRole.getId().equals("architect") || primaryRole.getId().equals("reviewer"))) {
                    plotSQL.addOrUpdateReviewer(uuid, primaryRole.getId());
                }
            }

            DiscordRole discordRole = new DiscordRole(uuid, roleId, !remove);
            customChat.sendSocketMessage(discordRole);

            if (announce && !remove) {
                sendPromotionChatMessage(name, role);
            }

            if (!remove) {
                sendPromotionDirectMessage(uuid, role);
            }

            if (remove) {
                return ChatUtils.success("Demoted %s from %s", name, roleId);
            } else {
                return ChatUtils.success("Promoted %s to %s", name, roleId);
            }
        });
    }

    private void sendPromotionChatMessage(String name, Role role) {
        Component message = Component.text(name)
                .append(PROMOTION_TEMPLATE)
                .append(role.getColouredRoleName())
                .decorate(TextDecoration.BOLD);
        customChat.sendChatMessage(new ChatMessage(GLOBAL.getChannelName(), "server", message));
    }

    private void sendPromotionDirectMessage(String uuid, Role role) {
        Component message = PROMOTION_SELF
                .append(role.getColouredRoleName())
                .decorate(TextDecoration.BOLD);
        customChat.sendDirectMessage(new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid
                , "server", message, true));
    }
}
