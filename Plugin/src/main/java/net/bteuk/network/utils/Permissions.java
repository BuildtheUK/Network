package net.bteuk.network.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static net.buildtheearth.terraminusminus.TerraMinusMinus.LOGGER;

/**
 * Utility class for permission related actions.
 */
public final class Permissions {

    private Permissions() {
    }

    private static LuckPerms getProvider() {
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        return provider == null ? null : provider.getProvider();
    }

    public static CompletableFuture<User> getUser(String uuid) {
        LuckPerms provider = getProvider();

        if (provider == null) {
            return null;
        }

        UserManager userManager = provider.getUserManager();
        return userManager.loadUser(UUID.fromString(uuid));
    }

    /**
     * Checks whether a group exists with the name provided.
     *
     * @param groupName the name of the group to check
     * @return {@link Group}
     */
    public static Group getGroup(String groupName) {
        LuckPerms provider = getProvider();

        if (provider == null) {
            LOGGER.warn("LuckPerms is required, but not available!");
            return null;
        }

        return provider.getGroupManager().getGroup(groupName);
    }

    /**
     * @param uuid   user to modify the group for
     * @param group  the group to modify
     * @param remove true if the group should be removed
     * @return the primary role after the modification
     */
    public static String modifyGroup(String uuid, Group group, boolean remove) {
        LuckPerms provider = getProvider();
        if (provider == null) {
            return null;
        }
        UserManager userManager = provider.getUserManager();
        User user = userManager.loadUser(UUID.fromString(uuid)).join();

        InheritanceNode node = InheritanceNode.builder(group).build();
        DataMutateResult result;
        if (remove) {
            result = user.data().remove(node);
        } else {
            result = user.data().add(node);
        }
        if (result.wasSuccessful()) {
            userManager.saveUser(user).join();
            return user.getPrimaryGroup();
        }
        return null;
    }

    public static CompletableFuture<String> getPrimaryGroup(String uuid) {
        LuckPerms provider = getProvider();

        if (provider == null) {
            return null;
        }

        UserManager userManager = provider.getUserManager();
        return userManager.loadUser(UUID.fromString(uuid)).thenApplyAsync(User::getPrimaryGroup);
    }

    public static CompletableFuture<Boolean> hasGroup(String uuid, String roleID) {
        LuckPerms provider = getProvider();

        if (provider == null) {
            return null;
        }

        UserManager userManager = provider.getUserManager();

        // Add group to user. Returns boolean to indicate success.
        return userManager.loadUser(UUID.fromString(uuid)).thenApplyAsync(user -> {
            CachedPermissionData permissionData = user.getCachedData().getPermissionData();
            return permissionData.checkPermission("group." + roleID).asBoolean();
        });
    }
}
