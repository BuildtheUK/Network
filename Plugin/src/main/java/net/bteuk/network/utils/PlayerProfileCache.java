package net.bteuk.network.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A cache for {@link PlayerProfile} objects to prevent excessive loading from the Mojang API.
 */
public class PlayerProfileCache {

    private static final int MAX_ENTRIES = 500;

    private static final AsyncCache<UUID, PlayerProfile> CACHE = Caffeine.newBuilder()
            .maximumSize(MAX_ENTRIES)
            .expireAfterWrite(Duration.ofHours(12))
            .buildAsync();

    /**
     * Gets a {@link PlayerProfile} for the given UUID, loading it if not already cached.
     *
     * @param uuid the UUID of the player
     * @return a CompletableFuture that will complete with the PlayerProfile
     */
    public static CompletableFuture<PlayerProfile> getProfile(UUID uuid) {
        return CACHE.get(uuid, PlayerProfileCache::loadProfileAsync);
    }

    private static CompletableFuture<PlayerProfile> loadProfileAsync(UUID uuid, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerProfile profile = Bukkit.createProfile(uuid);
            if (!profile.hasTextures()) {
                profile.complete();
            }
            return profile;
        }, executor);
    }
}
