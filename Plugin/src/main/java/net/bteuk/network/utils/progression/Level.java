// package net.bteuk.network.utils.progression;
//
// import net.bteuk.network.Network;
//
// public class Level {
//
//     protected static void addPlayerIfNotExists(String season, String uuid) {
//
//         if (!Network.getInstance().getGlobalSQL().hasRow("SELECT uuid FROM progression WHERE uuid='" + uuid + "' AND " +
//                 "season='" + season + "';")) {
//             Network.getInstance().getGlobalSQL().update("INSERT INTO progression(season,uuid) VALUES('" + season +
//                     "','" + uuid + "');");
//         }
//     }
//
//     /**
//      * Get the next exp threshold to reach the given level.
//      *
//      * @param level the level to get the threshold for
//      * @return the exp required to reach the level
//      */
//     protected static int getThreshold(int level) {
//
//         return (int) Math.round(50 * Math.log((2d * level) / 3d) + level);
//     }
//
//     /**
//      * Check whether the next level has been reached.
//      *
//      * @param currentLevel the current level
//      * @param exp          the current exp
//      * @return whether the next level has been reached
//      */
//     public static boolean reachedNextLevel(int currentLevel, int exp) {
//
//         return (exp >= getThreshold(currentLevel + 1));
//     }
//
//     /**
//      * Get the leftover exp after reaching the next level.
//      *
//      * @param level      the level that has been reached
//      * @param currentExp the current amount of exp
//      * @return the amount of exp left after leveling up
//      */
//     public static int getLeftoverExp(int level, int currentExp) {
//
//         return (currentExp - getThreshold(level));
//     }
//
//     /**
//      * Get the level of the player from the database.
//      *
//      * @param season the season to get the level for
//      * @param uuid   the uuid of the player
//      * @return the current level
//      */
//     public static int getLevel(String season, String uuid) {
//
//         return Network.getInstance().getGlobalSQL().getInt("SELECT lvl FROM progression WHERE uuid='" + uuid + "' AND" +
//                 " season='" + season + "';");
//     }
//
//     /**
//      * Get the exp of the player from the database.
//      *
//      * @param season the season to get the exp for
//      * @param uuid   the uuid of the player
//      * @return the current exp
//      */
//     public static int getExp(String season, String uuid) {
//
//         return Network.getInstance().getGlobalSQL().getInt("SELECT exp FROM progression WHERE uuid='" + uuid + "' AND" +
//                 " season='" + season + "';");
//     }
//
//     /**
//      * Set the level of the player in the database.
//      *
//      * @param season the season to set the level for
//      * @param uuid   the uuid of the player
//      * @param level  the level to set
//      * @return whether the action was successful
//      */
//     public static boolean setLevel(String season, String uuid, int level) {
//
//         return Network.getInstance().getGlobalSQL()
//                 .update("UPDATE progression SET lvl=" + level + " WHERE uuid='" + uuid + "' AND season='" + season +
//                         "';");
//     }
//
//     /**
//      * Set the exp of the player in the database.
//      *
//      * @param season the season to set the exp for
//      * @param uuid   the uuid of the player
//      * @param exp    the level to set
//      * @return whether the action was successful
//      */
//     public static boolean setExp(String season, String uuid, int exp) {
//
//         return Network.getInstance().getGlobalSQL()
//                 .update("UPDATE progression SET exp=" + exp + " WHERE uuid='" + uuid + "' AND season='" + season +
//                         "';");
//     }
// }
