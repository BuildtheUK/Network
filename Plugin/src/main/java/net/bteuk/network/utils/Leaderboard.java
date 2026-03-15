// package net.bteuk.network.utils;
//
// import net.bteuk.network.Network;
// import net.bteuk.network.sql.GlobalSQL;
// import net.bteuk.network.utils.enums.LeaderboardType;
// import net.bteuk.network.utils.enums.PointsType;
// import org.bukkit.entity.Player;
//
// import java.sql.ResultSet;
// import java.sql.SQLException;
//
// public class Leaderboard {
//
//     private final String[] aUuids = new String[9];
//     private final int[] aPoints = new int[9];
//     private final int[] aPosition = new int[9];
//
//     public void setLeaderboard(String uuid, PointsType pType, LeaderboardType lType) {
//
//         GlobalSQL globalSQL = Network.getInstance().getGlobalSQL();
//
//         String type = null;
//         int points;
//         int position;
//         int counter;
//
//         if (pType == PointsType.POINTS) {
//
//             type = "points";
//         } else if (pType == PointsType.BUILDING_POINTS) {
//
//             type = "building_points";
//         } else if (pType == PointsType.POINTS_WEEKLY) {
//
//             type = "points_weekly";
//             Points.updateWeekly();
//         } else if (pType == PointsType.BUILDING_POINTS_MONTHLY) {
//
//             type = "building_points_monthly";
//             Points.updateMonthly();
//         }
//
//         if (lType == LeaderboardType.USER) {
//
//             // Get points of user.
//             points = globalSQL.getInt("SELECT " + type + " FROM points_data WHERE uuid='" + uuid + "';");
//
//             // Get position on leaderboard of user.
//             // We can do this by counting all users with more points and adding 1.
//             position = 1 + globalSQL.getInt("SELECT count(uuid) FROM points_data WHERE " + type + ">" + points + ";");
//
//             // Get users of 4 nearest points values above the user.
//             ResultSet results = globalSQL.getResultSet("SELECT " + type + ",uuid FROM points_data WHERE "
//                     + type + ">" + points + " ORDER BY " + type + " ASC LIMIT 4;");
//
//             // If there are less than 4 values the leaderboard will have empty places.
//             // The resultset is in ascending order so we must start at place 4 and work our way back.
//             counter = 3;
//
//             try {
//
//                 while (results.next()) {
//
//                     aUuids[counter] =
//                             globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + results.getString(2) +
//                                     "';");
//                     aPoints[counter] = results.getInt(1);
//                     aPosition[counter] = position - (4 - counter);
//
//                     counter--;
//                 }
//
//                 // Close resultset
//                 results.close();
//             } catch (SQLException e) {
//                 e.printStackTrace();
//             }
//
//             // Add in user.
//             aUuids[4] = aUuids[counter] = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + uuid +
//                     "';");
//             aPoints[4] = points;
//             aPosition[4] = position;
//
//             // Get users of 4 nearest points values below the user.
//             results = globalSQL.getResultSet("SELECT " + type + ",uuid FROM points_data WHERE "
//                     + type + "<=" + points + " AND UUID <> '" + uuid + "' ORDER BY " + type + " DESC LIMIT 4;");
//
//             // If there are less than 4 values the leaderboard will have empty places.
//             counter = 5;
//
//             try {
//
//                 while (results.next()) {
//
//                     aUuids[counter] = aUuids[counter] = globalSQL.getString("SELECT name FROM player_data WHERE " +
//                             "uuid='" + results.getString(2) + "';");
//                     aPoints[counter] = results.getInt(1);
//                     aPosition[counter] = position + (counter - 4);
//
//                     counter++;
//                 }
//
//                 // Close resultset
//                 results.close();
//             } catch (SQLException e) {
//                 e.printStackTrace();
//             }
//         } else if (lType == LeaderboardType.TOP) {
//
//             // Get the top 9 users.
//             ResultSet results =
//                     globalSQL.getResultSet("SELECT " + type + ",uuid FROM points_data ORDER BY " + type + " DESC " +
//                             "LIMIT 9;");
//             counter = 0;
//
//             try {
//
//                 while (results.next()) {
//
//                     aUuids[counter] = aUuids[counter] = globalSQL.getString("SELECT name FROM player_data WHERE " +
//                             "uuid='" + results.getString(2) + "';");
//                     aPoints[counter] = results.getInt(1);
//                     aPosition[counter] = 1 + counter;
//
//                     counter++;
//                 }
//
//                 // Close resultset
//                 results.close();
//             } catch (SQLException e) {
//                 e.printStackTrace();
//             }
//         }
//     }
//
//     public void printLeaderboard(Player p) {
//
//         p.sendMessage(String.format("%-6s%-8s%-16s", "#", "Points", "Username"));
//         p.sendMessage("------------------------");
//
//         for (int i = 0; i < aUuids.length; i++) {
//
//             // If uuid is null skip this iteration.
//             if (aUuids[i] == null) {
//                 continue;
//             }
//
//             p.sendMessage(String.format("%-6s%-8s%-16s", aPosition[i], aPoints[i], aUuids[i]));
//         }
//     }
// }
