// package net.bteuk.network.utils;
//
// import net.bteuk.network.Network;
// import net.bteuk.network.sql.GlobalSQL;
// import net.bteuk.network.utils.enums.PointsType;
//
// import java.util.ArrayList;
//
// public class Points {
//
//     public static void addPoints(String uuid, int points, PointsType type) {
//
//         GlobalSQL globalSQL = Network.getInstance().getGlobalSQL();
//
//         if (type == PointsType.POINTS) {
//
//             // If the player has points for today, add them.
//             // Else create new value.
//             if (globalSQL.hasRow("SELECT uuid FROM points_info WHERE uuid='" + uuid +
//                     "' AND type='POINTS' AND on_date=DATE " + Time.getDate(Time.currentTime()) + ";")) {
//
//                 globalSQL.update("UPDATE points_info SET points=points+" + points +
//                         " WHERE uuid='" + uuid + "' AND type='POINTS' AND on_date=DATE " + Time.getDate(
//                         Time.currentTime()) + ";");
//             } else {
//
//                 globalSQL.update("INSERT INTO points_info(uuid,type,on_date,points) VALUES('" +
//                         uuid + "','POINTS',DATE " + Time.getDate(Time.currentTime()) + "," + points + ");");
//             }
//
//             // Add to total points.
//             globalSQL.update("UPDATE points_data SET points=points+" + points + " WHERE uuid='" + uuid + "';");
//
//             // Update weekly points.
//             long time = Time.currentTime() - (1000L * 60 * 60 * 24 * 7);
//             int wPoints = globalSQL.getInt("SELECT SUM(points) FROM points_info WHERE uuid='" + uuid +
//                     "' AND type='POINTS' AND on_date < DATE " + Time.getDate(time) + ";");
//             globalSQL.update("UPDATE points_data SET points_weekly=" + wPoints + " WHERE uuid='" + uuid + "';");
//         } else if (type == PointsType.BUILDING_POINTS) {
//
//             // If the player has building points for today, add them.
//             // Else create new value.
//             if (globalSQL.hasRow("SELECT uuid FROM points_info WHERE uuid='" + uuid +
//                     "' AND type='BUILDING_POINTS' AND on_date=DATE " + Time.getDate(Time.currentTime()) + ";")) {
//
//                 globalSQL.update("UPDATE points_info SET points=points+" + points +
//                         " WHERE uuid='" + uuid + "' AND type='BUILDING_POINTS' AND on_date=DATE " + Time.getDate(
//                         Time.currentTime()) + ";");
//             } else {
//
//                 globalSQL.update("INSERT INTO points_info(uuid,type,on_date,points) VALUES('" +
//                         uuid + "','BUILDING_POINTS',DATE " + Time.getDate(Time.currentTime()) + "," + points + ");");
//             }
//
//             // Add to total building points.
//             globalSQL.update(
//                     "UPDATE points_data SET building_points=building_points+" + points + " WHERE uuid='" + uuid + "';");
//
//             // Update monthly building points.
//             long time = Time.currentTime() - (1000L * 60 * 60 * 24 * 30);
//             int wPoints = globalSQL.getInt("SELECT SUM(points) FROM points_info WHERE uuid='" + uuid +
//                     "' AND type='BUILDING_POINTS' AND on_date < DATE " + Time.getDate(time) + ";");
//             globalSQL.update("UPDATE points_data SET points_weekly=" + wPoints + " WHERE uuid='" + uuid + "';");
//         }
//     }
//
//     public static void updateWeekly() {
//
//         GlobalSQL globalSQL = Network.getInstance().getGlobalSQL();
//
//         // Get all users.
//         ArrayList<String> uuids = globalSQL.getStringList("SELECT uuid FROM points_data;");
//
//         // Update weekly points.
//         long time = Time.currentTime() - (1000L * 60 * 60 * 24 * 7);
//         for (String uuid : uuids) {
//
//             int wPoints = globalSQL.getInt("SELECT SUM(points) FROM points_info WHERE uuid='" + uuid +
//                     "' AND type='POINTS' AND on_date < DATE " + Time.getDate(time) + ";");
//             globalSQL.update("UPDATE points_data SET points_weekly=" + wPoints + " WHERE uuid='" + uuid + "';");
//         }
//
//         // Clear all data from before 7 days ago.
//         globalSQL.update("DELETE FROM points_info WHERE type='POINTS' AND on_date >= DATE " + Time.getDate(time) + ";");
//     }
//
//     public static void updateMonthly() {
//
//         GlobalSQL globalSQL = Network.getInstance().getGlobalSQL();
//
//         // Get all users.
//         ArrayList<String> uuids = globalSQL.getStringList("SELECT uuid FROM points_data;");
//
//         // Update monthly building points.
//         long time = Time.currentTime() - (1000L * 60 * 60 * 24 * 30);
//         for (String uuid : uuids) {
//
//             int wPoints = globalSQL.getInt("SELECT SUM(points) FROM points_info WHERE uuid='" + uuid +
//                     "' AND type='BUILDING_POINTS' AND on_date < DATE " + Time.getDate(time) + ";");
//             globalSQL.update("UPDATE points_data SET points_weekly=" + wPoints + " WHERE uuid='" + uuid + "';");
//         }
//
//         // Clear all data from before 30 days ago.
//         globalSQL.update(
//                 "DELETE FROM points_info WHERE type='BUILDING_POINTS' AND on_date >= DATE " + Time.getDate(time) + ";");
//     }
// }
