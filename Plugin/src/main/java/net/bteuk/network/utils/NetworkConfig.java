package net.bteuk.network.utils;

import lombok.Getter;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import org.apache.logging.log4j.util.Strings;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Log
public class NetworkConfig {

    @Getter
    private FileConfiguration config;

    private final Network instance;

    public NetworkConfig(Network instance) {
        this.instance = instance;

        // Create a config instance.
        config = instance.getConfig();
    }

    // Get old config version.
    private String configVersion() {

        String version = config.getString("version");
        // If null return default.
        return Objects.requireNonNullElse(version, "1.0.0");
    }

    // Get latest config version.
    private String latestVersion() {
        String version = Objects.requireNonNull(config.getDefaults()).getString("version");
        // If null return default.
        return Objects.requireNonNullElse(version, "1.11.0-SNAPSHOT");
    }

    // Update config if the version is outdated.
    public void updateConfig() {

        String version = configVersion();

        if (!version.equals(latestVersion())) {
            log.info("Your config version is outdated, updating to latest version!");

            // Get old config values, these are needed to add them back after updating.
            Map<String, Object> values = config.getValues(true);

            // Migrate the values if necessary.
            migrateConfig(values, ConfigVersion.parse(version), ConfigVersion.parse(latestVersion()));

            // Generate a new config file from the default config.
            // Copy any values that can be reused.
            // Delete the current config and set the new one.
            File configFile = new File(instance.getDataFolder(), "config.yml");

            if (!configFile.delete()) {

                // Something went wrong.
                log.warning("The old config file could not be deleted!");
                return;
            }

            // Copy the default config and get it.
            instance.saveDefaultConfig();
            instance.reloadConfig();
            config = instance.getConfig();

            for (Map.Entry<String, Object> value : values.entrySet()) {

                if (config.contains(value.getKey())) {

                    // Check if this is a configuration section, if true skip.
                    if (config.isConfigurationSection(value.getKey())) {
                        continue;
                    }

                    // Skip the version since that needs to be the latest value.
                    if (value.getKey().equals("version")) {
                        continue;
                    }
                    config.set(value.getKey(), value.getValue());
                }
            }

            instance.saveConfig();
            config = instance.getConfig();
            log.info("Updated config to version " + config.getString("version"));
        } else {
            log.info("The config is up to date!");
        }
    }

    /**
     * Migrates configuration values based on defined migration rules.
     *
     * @param values          the current configuration values
     * @param previousVersion the version of the config before the update
     * @param currentVersion  the version of the config after the update
     */
    private void migrateConfig(Map<String, Object> values, ConfigVersion previousVersion, ConfigVersion currentVersion) {
        for (ConfigMigration migration : ConfigMigration.values()) {
            // Check if the migration should be executed.
            // The migration is executed if the previous version is before the migration version,
            // and the current version is at or after the migration version.
            if (previousVersion.compareTo(migration.getVersion()) < 0 && currentVersion.compareTo(migration.getVersion()) >= 0) {
                // If the old key exists, migrate it to the new key.
                if (values.containsKey(migration.getOldKey())) {
                    Object value = values.remove(migration.getOldKey());
                    values.put(migration.getNewKey(), value);
                    log.info("Migrated config key '" + migration.getOldKey() + "' to '" + migration.getNewKey() + "' (migration version " + migration.getVersion() + ")");
                }
            }
        }
    }

    public net.bteuk.network.core.Constants getConstants() {
        log.info("Loading constants from config...");

        // Set the server name from config.
        String serverName = config.getString("server_name");

        // Set the server type from config.
        ServerType serverType = ServerType.valueOf(config.getString("server_type"));

        // Basically indicates that this server is not running in a network.
        boolean standalone = config.getBoolean("standalone");

        boolean regionsEnabled = config.getBoolean("regions.enabled");

        // days * 24 hours * 60 minutes * 60 seconds * 1000 milliseconds
        long regionInactivity = config.getInt("regions.inactivity_days") * 24L * 60L * 60L * 1000L;

        boolean tpllEnabled = config.getBoolean("tpll.enabled");
        boolean tpllRequiredPermission = config.getBoolean("tpll.requires_permission");

        int maxY = config.getInt("tpll.max_y");
        int minY = config.getInt("tpll.min_y");

        boolean staffChat = config.getBoolean("staff.chat.enabled");

        boolean tips = config.getBoolean("chat.tips.enabled");

        boolean tutorials = config.getBoolean("tutorials.enabled");
        boolean compulsoryTutorial = config.getBoolean("tutorials.compulsory_tutorial");

        boolean llEnabled = config.getBoolean("ll_enabled");

        boolean progressMap = config.getBoolean("ProgressMap.enabled");

        boolean progression = config.getBoolean("progression.enabled");
        boolean announceOverallLevelUps = config.getBoolean("progression.announce_level-ups.overall");
        boolean announceSeasonalLevelUps = config.getBoolean("progression.announce_level-ups.seasonal");

        boolean sidebarEnabled = config.getBoolean("sidebar.enabled");
        String sidebarTitle = config.getString("sidebar.title", Strings.EMPTY);

        List<?> sidebarTextConfig = config.getList("sidebar.text");
        List<String> sidebarText = new ArrayList<>();
        if (sidebarTextConfig != null && !sidebarTextConfig.isEmpty()) {
            sidebarTextConfig.forEach(listItem -> {
                if (listItem instanceof String listTextItem) {
                    sidebarText.add(listTextItem);
                }
            });
        }

        List<String> sidebarTextList = Collections.unmodifiableList(sidebarText);

        boolean motdEnabled = config.getBoolean("motd.enabled");
        String motdText = config.getString("motd.text", "");

        String earthWorld = config.getString("regions.earth_world", "earth");

        String minrankGeneration = config.getString("minrank_generation", "Jr.Builder");
        String minrankRegionClaim = config.getString("minrank_regionclaim", "Jr.Builder");
        String minrankZoneJoin = config.getString("minrank_zonejoin", "Jr.Builder");

        boolean plotSystemEnabled = config.getBoolean("plot_system_enabled");

        boolean moderationEnabled = config.getBoolean("staff.moderation.enabled");

        boolean warpsEnabled = config.getBoolean("warps_enabled");

        boolean homesEnabled = config.getBoolean("homes_enabled");

        boolean announcePromotions = config.getBoolean("chat.announce_promotions");

        boolean skullsEnabled = config.getBoolean("skulls_plugin_enabled");

        String chatSocketOutputIP = config.getString("chat.socket.output.IP");
        int chatSocketOutputPort = config.getInt("chat.socket.output.port");
        int chatSocketInputPort = config.getInt("chat.socket.input.port");

        int tipsFrequency = config.getInt("chat.tips.frequency");

        boolean regionStaffRequestAlways = config.getBoolean("regions.staff_request.always");
        int regionStaffRequestRadius = config.getInt("regions.staff_request.radius", 0);

        int navigationRadius = config.getInt("navigation_radius", 200);

        int afkTime = config.getInt("afk", 5);

        boolean mapEnabled = config.getBoolean("map.enabled");
        String mapServer = config.getString("map.server");
        NetworkLocation mapLocation = new NetworkLocation(config.getString("map.location.world"), config.getDouble("map.location.x", 0), config.getDouble("map.location.y", 0),
                config.getDouble("map.location.z", 0), (float) config.getDouble("map.location.yaw", 0),
                (float) config.getDouble("map.location.pitch", 0));

        NetworkLocation spawnLocation = new NetworkLocation(config.getString("spawn.world"), config.getDouble("spawn.x"), config.getDouble("spawn.y"),
                config.getDouble("spawn.z"), (float) config.getDouble("spawn.yaw"), (float) config.getDouble("spawn.pitch"));

        boolean UKSurvey = config.getBoolean("UKSurvey_enabled");

        String discordLink = config.getString("links.discord", null);
        String websiteLink = config.getString("links.website", null);
        String progressMapLink = config.getString("links.progress_map", null);

        log.info("Loaded constants from config.");
        return new Constants(serverName, serverType, standalone, regionsEnabled, regionInactivity, tpllEnabled, tpllRequiredPermission, maxY, minY, earthWorld, staffChat, tips,
                tutorials, llEnabled, progression, announceOverallLevelUps, announceSeasonalLevelUps, sidebarEnabled, sidebarTitle, sidebarTextList, motdEnabled,
                motdText, minrankGeneration, minrankRegionClaim, minrankZoneJoin, plotSystemEnabled, moderationEnabled, warpsEnabled, homesEnabled, announcePromotions,
                skullsEnabled, chatSocketOutputIP,
                chatSocketOutputPort, chatSocketInputPort, tipsFrequency, regionStaffRequestAlways, regionStaffRequestRadius, navigationRadius,
                compulsoryTutorial, afkTime, mapEnabled, mapServer, mapLocation, spawnLocation, UKSurvey, discordLink, websiteLink, progressMapLink);
    }
}