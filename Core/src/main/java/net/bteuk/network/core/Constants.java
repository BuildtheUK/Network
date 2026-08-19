package net.bteuk.network.core;

import net.bteuk.network.api.entity.NetworkLocation;

import java.util.List;

public record Constants(String serverName, ServerType serverType, boolean standalone, boolean regionsEnabled, long regionInactivity, boolean tpllEnabled,
                        boolean tpllRequiresPermission, int maxY, int minY, boolean staffChat, boolean tips, boolean tutorials, boolean ll,
                        boolean progression, boolean announceOveralLevelUps, boolean announceSeasonLevelUps, boolean sidebarEnabled, String sidebarTitle,
                        List<String> sidebarContent, boolean motdEnabled, String motdContent, String minrankGeneration, String minrankRegionClaim, String minrankZoneJoin,
                        boolean plotSystemEnabled, boolean moderationEnabled, boolean warpsEnabled,
                        boolean homesEnabled, boolean announcePromotions, boolean skullsEnabled, String chatSocketOutputIP,
                        int chatSocketOutputPort, int chatSocketInputPort, int tipsFrequency, boolean regionStaffRequestAlways, int regionStaffRequestRadius,
                        int navigationRadius, boolean compulsoryTutorial, int afkTime, boolean mapEnabled, String mapServer, NetworkLocation mapLocation,
                        NetworkLocation spawnLocation, boolean UKSurvey, String discordLink, String websiteLink, String progressMapLink, String earthDimension) {
}