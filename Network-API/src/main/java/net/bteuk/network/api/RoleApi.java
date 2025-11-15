package net.bteuk.network.api;

import net.bteuk.network.api.entity.Role;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface RoleApi {

    Set<Role> getRoles();

    /**
     * Get the builder role for a potentially offline player.
     *
     * @param uuid the uuid of the player
     * @return a {@link CompletableFuture} with a String
     */
    CompletableFuture<String> getBuilderRole(String uuid);

    CompletableFuture<Component> alterRole(String uuid, String name, String roleId, boolean remove, boolean announce);
}
