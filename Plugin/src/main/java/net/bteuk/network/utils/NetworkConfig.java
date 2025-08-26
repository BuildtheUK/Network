package net.bteuk.network.utils;

import lombok.Getter;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
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
        return Objects.requireNonNullElse(version, "1.8.0");
    }

    // Update config if the version is outdated.
    public void updateConfig() {

        String version = configVersion();

        if (!version.equals(latestVersion())) {
            log.info("Your config version is outdated, updating to latest version!");

            // Get old config values, these are needed to add them back after updating.
            Map<String, Object> values = config.getValues(true);

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
        long regionInactivity = config.getInt("region.inactivity_days") * 24L * 60L * 60L * 1000L;

        boolean tpllEnabled = config.getBoolean("tpll.enabled");
        boolean tpllRequiredPermission = config.getBoolean("tpll.requires_permission");

        int maxY = config.getInt("tpll.max_y");
        int minY = config.getInt("tpll.min_y");

        boolean staffChat = config.getBoolean("staff.chat.enabled");

        boolean tips = config.getBoolean("chat.tips.enabled");

        boolean tutorials = config.getBoolean("tutorials.enabled");

        boolean llEnabled = config.getBoolean("ll_enabled");

        boolean progressMap = config.getBoolean("ProgressMap.enabled");

        boolean progression = config.getBoolean("progression.enabled");
        boolean announceOverallLevelUps = config.getBoolean("progression.announce_level-ups.overall");
        boolean announceSeasonalLevelUps = config.getBoolean("progression.announce_level-ups.seasonal");

        boolean sidebarEnabled = config.getBoolean("sidebar.enabled");
        String sidebarTitle = config.getString("sidebar.title", "");

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

        String earthWorld;
        if (config.getString("regions.earth_world") == null) {
            // Setting default value.
            earthWorld = "earth";
        } else {
            earthWorld = config.getString("regions.earth_world");
        }

        boolean plotSystemEnabled = config.getBoolean("plot_system.enabled");

        boolean moderationEnabled = config.getBoolean("staff.moderation.enabled");

        boolean warpsEnabled = config.getBoolean("warps_enabled");

        boolean homesEnabled = config.getBoolean("homes_enabled");

        boolean announcePromotions = config.getBoolean("chat.announce_promotions");

        String discordLink = config.getString("discord");

        log.info("Loaded constants from config.");
        return new Constants(serverName, serverType, standalone, regionsEnabled, regionInactivity, tpllEnabled, tpllRequiredPermission, maxY, minY, earthWorld, staffChat, tips,
                tutorials, llEnabled, progressMap, progression, announceOverallLevelUps, announceSeasonalLevelUps, sidebarEnabled, sidebarTitle, sidebarTextList, motdEnabled,
                motdText, plotSystemEnabled, moderationEnabled, warpsEnabled, homesEnabled, announcePromotions, discordLink);
    }
}