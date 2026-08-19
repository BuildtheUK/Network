// package net.bteuk.network.utils.progression;
//
// import net.bteuk.network.Network;
// import org.btuk.network.lib.dto.ChatMessage;
// import org.btuk.network.lib.enums.ChatChannels;
// import org.btuk.network.lib.utils.ChatUtils;
// import net.bteuk.network.utils.NetworkUser;
// import net.kyori.adventure.text.Component;
// import net.kyori.adventure.text.format.NamedTextColor;
//
// public class Progression {
//
//     /**
//      * Add exp for the player to all active seasons. If the player levels up, this will be processed and announced.
//      *
//      * @param uuid the uuid of the player
//      * @param exp  the amount of exp to add
//      */
//     public static void addExp(String uuid, int exp) {
//
//         // If progression is disabled, cancel.
//         if (!PROGRESSION) {
//             return;
//         }
//
//         // Add exp for the overall progression.
//         addExp("default", uuid, exp, ANNOUNCE_OVERALL_LEVELUPS);
//
//         // Add exp for the active season, if it exists. There can only be 1 active season at a time.
//         // Don't use the default season, this is always active.
//         if (Network.getInstance().getGlobalSQL().hasRow("SELECT id FROM seasons WHERE active=1 and id<>'default'")) {
//             addExp(Network.getInstance().getGlobalSQL().getString("SELECT id FROM seasons WHERE active=1 and " +
//                     "id<>'default'"), uuid, exp, ANNOUNCE_SEASONAL_LEVELUPS);
//         }
//     }
//
//     /**
//      * Add exp for the player to a specific season. If the player levels up, this will be processed and announced.
//      *
//      * @param season            the season to add the exp to.
//      * @param uuid              the uuid of the player
//      * @param exp               the amount of exp to add
//      * @param announce_levelups whether level-ups should be announced
//      */
//     private static void addExp(String season, String uuid, int exp, boolean announce_levelups) {
//
//         Level.addPlayerIfNotExists(season, uuid);
//
//         int currentExp = exp + Level.getExp(season, uuid);
//         int currentLevel = Level.getLevel(season, uuid);
//
//         while (Level.reachedNextLevel(currentLevel, currentExp)) {
//
//             // Increase level.
//             currentLevel++;
//             levelUp(season, uuid, currentLevel, announce_levelups);
//
//             // Get remainig exp.
//             currentExp = Level.getLeftoverExp(currentLevel, currentExp);
//         }
//
//         // Set exp.
//         Level.setExp(season, uuid, currentExp);
//     }
//
//     private static void levelUp(String season, String uuid, int level, boolean announce_levelup) {
//
//         Level.setLevel(season, uuid, level);
//
//         Component playerMessage = ChatUtils.success("You have reached level ")
//                 .append(Component.text(level, NamedTextColor.DARK_AQUA));
//
//         Component globalMessage = Component.text(NetworkUser.getName(uuid), NamedTextColor.DARK_AQUA)
//                 .append(ChatUtils.success(" has reached level ")
//                         .append(Component.text(level, NamedTextColor.DARK_AQUA)));
//
//         if (!season.equals("default")) {
//
//             playerMessage = playerMessage.append(ChatUtils.success(" in season ")
//                     .append(Component.text(season, NamedTextColor.DARK_AQUA)));
//
//             globalMessage = globalMessage.append(ChatUtils.success(" in season ")
//                     .append(Component.text(season, NamedTextColor.DARK_AQUA)));
//         }
//
//         // Announce the levelup if enabled.
//         if (announce_levelup) {
//
//             // Announce level-up.
//             ChatMessage chatMessage = new ChatMessage(ChatChannels.GLOBAL.getChannelName(), "server", globalMessage);
//             Network.getInstance().getChat().sendSocketMessage(chatMessage);
//         }
//
//         // Send a message to the player.
//         NetworkUser.sendOfflineMessage(uuid, playerMessage);
//     }
// }
