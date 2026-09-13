package net.bteuk.network.utils;

import lombok.Getter;

/**
 * An enum representing configuration migrations.
 * Each constant defines a version at which a key rename occurred.
 */
@Getter
public enum ConfigMigration {

    DISCORD_LINK(new ConfigVersion(1, 11, 0), "discord", "links.discord"),
    WARPS_ENABLED(new ConfigVersion(1, 11, 0), "warps_enabled", "explore.warps_enabled");

    private final ConfigVersion version;
    private final String oldKey;
    private final String newKey;

    ConfigMigration(ConfigVersion version, String oldKey, String newKey) {
        this.version = version;
        this.oldKey = oldKey;
        this.newKey = newKey;
    }
}
