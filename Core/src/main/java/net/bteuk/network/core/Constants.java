package net.bteuk.network.core;

import java.util.List;

public record Constants(String serverName, ServerType serverType, boolean standalone, boolean regionsEnabled, long regionInactivity, boolean tpllEnabled, boolean tpllRequiresPermission, int maxY, int minY, String earthWorld,
                        boolean staffChat, boolean tips, boolean tutorials, boolean ll, boolean progressMap, boolean progression, boolean announceOveralLevelUps,
                        boolean announceSeasonLevelUps, boolean sidebarEnabled, String sidebarTitle, List<String> sidebarContent, boolean motdEnabled, String motdContent, boolean plotSystemEnabled,
                        boolean moderationEnabled) { }
