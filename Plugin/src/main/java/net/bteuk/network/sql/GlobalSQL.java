package net.bteuk.network.sql;

import lombok.extern.java.Log;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.building_counter.Building;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.core.sql.AbstractSQL;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.survey.Survey;
import net.bteuk.network.utils.Coordinate;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

@Log
public class GlobalSQL extends AbstractSQL {

    private final Constants constants;

    public GlobalSQL(DataSource datasource, Constants constants) {
        super(datasource);
        this.constants = constants;
    }

    // Get a hashmap of all events for this server.
    public ArrayList<String[]> getEvents(String serverName, ArrayList<String[]> list) {

        // Try and get all events for this server.
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("SELECT uuid,event,message FROM server_events " + "WHERE server='" + serverName + "';");
                ResultSet results = statement.executeQuery()
        ) {

            while (results.next()) {

                list.add(new String[]{results.getString(1), results.getString(2), results.getString(3)});
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to fetch server events for " + serverName, e);
            return list;
        }

        // Try and delete all events for this server.
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("DELETE FROM server_events WHERE server='" + serverName + "';")
        ) {

            statement.executeUpdate();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to delete server events for " + serverName, e);
            return list;
        }

        // Return the map.
        return list;
    }

    // Add new coordinate to database and return the id.
    public int addCoordinate(Location l) {

        return addCoordinate(constants.serverName(), l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    // Add new coordinate to database and return the id.
    public int addCoordinate(NetworkLocation l) {
        return addCoordinate(constants.serverName(), l.world(), l.x(), l.y(), l.z(), l.yaw(), l.pitch());
    }

    public int addCoordinate(Coordinate coordinate) {
        return addCoordinate(coordinate.getServer(), coordinate.getWorld(), coordinate.getX(), coordinate.getY(), coordinate.getZ(), coordinate.getYaw(), coordinate.getPitch());
    }

    // Add new coordinate to database and return the id.
    public int addCoordinate(String server, Location l) {
        return addCoordinate(server, l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
    }

    // Add new coordinate using values, rather than location.
    public int addCoordinate(String server, String world, double x, double y, double z, float yaw, float pitch) {

        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO coordinates(server,world, x, y, z, yaw, pitch) VALUES(?, ?, ?, ?, ?, ?, ?);",
                        Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setString(1, server);
            statement.setString(2, world);
            statement.setDouble(3, x);
            statement.setDouble(4, y);
            statement.setDouble(5, z);
            statement.setFloat(6, yaw);
            statement.setFloat(7, pitch);
            statement.executeUpdate();

            // If the id does not exist return 0.
            ResultSet results = statement.getGeneratedKeys();
            if (results.next()) {

                return results.getInt(1);
            } else {

                return 0;
            }
        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to add coordinate", sql);
            return 0;
        }
    }

    // Update an existing coordinate.
    public void updateCoordinate(int coordinateID, String server, Location l) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("UPDATE coordinates SET server=?, world=?, x=?, y=?, z=?, yaw=?, pitch=? WHERE id=?;")
        ) {
            statement.setString(1, server);
            statement.setString(2, l.getWorld().getName());
            statement.setDouble(3, l.getX());
            statement.setDouble(4, l.getY());
            statement.setDouble(5, l.getZ());
            statement.setFloat(6, l.getYaw());
            statement.setFloat(7, l.getPitch());
            statement.setInt(8, coordinateID);
            statement.executeUpdate();
        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to update coordinate " + coordinateID, sql);
        }
    }

    // Update an existing coordinate.
    public void updateCoordinate(int coordinateID, String server, NetworkLocation location) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("UPDATE coordinates SET server=?, world=?, x=?, y=?, z=?, yaw=?, pitch=? WHERE id=?;")
        ) {
            statement.setString(1, server);
            statement.setString(2, location.world());
            statement.setDouble(3, location.x());
            statement.setDouble(4, location.y());
            statement.setDouble(5, location.z());
            statement.setFloat(6, location.yaw());
            statement.setFloat(7, location.pitch());
            statement.setInt(8, coordinateID);
            statement.executeUpdate();
        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to update coordinate " + coordinateID, sql);
        }
    }

    // Update an existing coordinate.
    public void updateCoordinate(int coordinateID, Location l) {
        updateCoordinate(coordinateID, constants.serverName(), l);
    }

    // Update an existing coordinate.
    public void updateCoordinate(int coordinateID, NetworkLocation location) {
        updateCoordinate(coordinateID, constants.serverName(), location);
    }

    public ArrayList<Building> getBuildings(String condition) {
        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
                String.format("SELECT * FROM buildings INNER JOIN coordinates ON buildings.coordinate_id = coordinates.id %s;", condition));
                ResultSet results = statement.executeQuery()
        ) {
            ArrayList<Building> buildings = new ArrayList<>();
            while (results.next()) {
                Location temp = new Location(Bukkit.getWorld(results.getString("world")), results.getDouble("x"), results.getDouble("y"), results.getDouble("z"),
                        results.getFloat("yaw"), results.getFloat("pitch"));
                buildings.add(new Building(results.getInt("building_id"), temp, results.getString("player_id"), results.getInt("coordinate_id"),
                        results.getObject("time_added", LocalDateTime.class), results.getBoolean("is_public"), results.getBoolean("player_built"), results.getDouble("lat"),
                        results.getDouble("lon")));
            }
            return buildings;
        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to fetch buildings with condition: " + condition, sql);
            return null;
        }
    }

    // Get coordinate from database by id.
    // World must be on this server else this will throw a null pointer exception.
    public Location getLocation(int coordinateID) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("SELECT * FROM coordinates WHERE id=" + coordinateID + ";");
                ResultSet results = statement.executeQuery()
        ) {

            results.next();
            return (new Location(Bukkit.getWorld(results.getString("world")), results.getDouble("x"), results.getDouble("y"), results.getDouble("z"), results.getFloat("yaw"),
                    results.getFloat("pitch")));
        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to fetch location for coordinate " + coordinateID, sql);
            return null;
        }
    }

    // Get coordinate from database by id.
    // World must be on this server else this will throw a null pointer exception.
    public Coordinate getCoordinate(int coordinateID) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("SELECT * FROM coordinates WHERE id=" + coordinateID + ";");
                ResultSet results = statement.executeQuery()
        ) {

            results.next();
            return (new Coordinate(coordinateID, results.getString("server"), results.getString("world"), results.getDouble("x"), results.getDouble("y"), results.getDouble("z"),
                    results.getFloat("yaw"), results.getFloat("pitch")));
        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to fetch coordinate " + coordinateID, sql);
            return null;
        }
    }

    public void deleteBuilding(Building b) {
        String deleteBuildingSQL = "DELETE FROM buildings WHERE building_id = ?";
        String deleteCoordinatesSQL = "DELETE FROM coordinates WHERE id = ?";

        try (
                Connection conn = conn(); PreparedStatement deleteBuildingStatement = conn.prepareStatement(deleteBuildingSQL);
                PreparedStatement deleteCoordinatesStatement = conn.prepareStatement(deleteCoordinatesSQL)
        ) {
            conn.setAutoCommit(false); // Start transaction

            deleteBuildingStatement.setInt(1, b.buildingId());
            deleteBuildingStatement.executeUpdate();

            deleteCoordinatesStatement.setInt(1, b.coordinateId());
            deleteCoordinatesStatement.executeUpdate();

            conn.commit(); // Commit if both deletions succeed

        } catch (SQLException sql) {
            log.log(Level.SEVERE, "Failed to delete building " + b.buildingId(), sql);
        }
    }

    public boolean insertMessage(DirectMessage directMessage) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "INSERT INTO messages(recipient,message) VALUES(?,?);"
                )
        ) {
            statement.setString(1, directMessage.getRecipient());
            statement.setString(2, GsonComponentSerializer.gson().serialize(directMessage.getComponent()));

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to insert direct message for recipient " + directMessage.getRecipient(), e);
            return false;
        }
    }

    public List<String> getOfflineMessages(String uuid) {
        List<String> messages = new ArrayList<>();
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT message FROM messages WHERE recipient=?;"
                )
        ) {
            statement.setString(1, uuid);
            ResultSet results = statement.executeQuery();

            while (results.next()) {
                messages.add(results.getString(1));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to fetch offline messages for recipient " + uuid, e);
        }
        return messages;
    }

    public boolean createUser(String uuid, String name, String playerSkin) {

        int iPlayerThere = getInt("SELECT count(1) FROM player_data WHERE uuid=?", uuid);

        if (iPlayerThere == 0) {
            return update("INSERT INTO player_data(uuid,name,last_online,last_submit,player_skin) VALUES(?,?,?,?,?);",
                    uuid, name, Time.currentTime(), 0, playerSkin);
        } else
            return true;
    }

    public Survey getSurveyOfUser(UUID uuid) {
        Survey survey;
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT survey_completed_at, survey_last_edited, found_via_btuk, found_via_bte, found_via_btuk_external, found_via_bte_external,found_via_friend," +
                                "medium_tiktok, medium_youtube_shorts, medium_youtube_longform, medium_instagram," +
                                "medium_search_engine_browsing, medium_online_news, medium_tvnews, medium_physical_newspaper," +
                                "socials_tiktok, socials_youtube_shorts, socials_youtube_longform, socials_instagram FROM survey WHERE player=?;"
                )
        ) {
            statement.setString(1, uuid.toString());
            ResultSet results = statement.executeQuery();

            if (results.next()) {
                survey = new Survey(true, results.getTimestamp(1), results.getTimestamp(2),
                        results.getBoolean(3), results.getBoolean(4), results.getBoolean(5), results.getBoolean(6), results.getBoolean(7),
                        results.getBoolean(8), results.getBoolean(9), results.getBoolean(10), results.getBoolean(11),
                        results.getBoolean(12), results.getBoolean(13), results.getBoolean(14), results.getBoolean(15),
                        results.getBoolean(16), results.getBoolean(17), results.getBoolean(18), results.getBoolean(19));
                return survey;
            } else
                return null;

        } catch (SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
    }

    public void saveSurveyOfUser(UUID user, Survey survey) {
        try (
                Connection conn = conn();
                PreparedStatement statement = (survey.isExisting()) ?
                        conn.prepareStatement("UPDATE survey SET survey_last_edited=?, found_via_btuk=?, found_via_bte=?, " +
                                "found_via_btuk_external=?, found_via_bte_external=?, found_via_friend=?," +
                                "medium_tiktok=?, medium_youtube_shorts=?, medium_youtube_longform=?, medium_instagram=?," +
                                "medium_search_engine_browsing=?, medium_online_news=?, medium_tvnews=?, medium_physical_newspaper=?," +
                                "socials_tiktok=?, socials_youtube_shorts=?, socials_youtube_longform=?, socials_instagram=? WHERE player=?;")
                        :
                        conn.prepareStatement("INSERT INTO survey (survey_completed_at, survey_last_edited, found_via_btuk, found_via_bte, " +
                        "found_via_btuk_external,  found_via_bte_external, found_via_friend, " +
                        "medium_tiktok, medium_youtube_shorts, medium_youtube_longform, medium_instagram, " +
                        "medium_search_engine_browsing, medium_online_news, medium_tvnews, medium_physical_newspaper, " +
                        "socials_tiktok, socials_youtube_shorts, socials_youtube_longform, socials_instagram, player)" +
                        "VALUES (NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);")
                ) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setBoolean(2, survey.isBFoundViaBTUK());
            statement.setBoolean(3, survey.isBFoundViaBTE());
            statement.setBoolean(4, survey.isBFoundViaBTUKExternal());
            statement.setBoolean(5, survey.isBFoundViaBTEExternal());
            statement.setBoolean(6, survey.isBFoundViaFriend());

            statement.setBoolean(7, survey.isBMediumTiktok());
            statement.setBoolean(8, survey.isBMediumYoutubeShorts());
            statement.setBoolean(9, survey.isBMediumYoutubeLongform());
            statement.setBoolean(10, survey.isBMediumInstagram());
            statement.setBoolean(11, survey.isBSearchEngineBrowsing());
            statement.setBoolean(12, survey.isBOnlineNews());
            statement.setBoolean(13, survey.isBTVNews());
            statement.setBoolean(14, survey.isBPhysicalNewspaper());

            statement.setBoolean(15, survey.isBSocialsTiktok());
            statement.setBoolean(16, survey.isBSocialsYoutubeShorts());
            statement.setBoolean(17, survey.isBSocialsYoutubeLongform());
            statement.setBoolean(18, survey.isBSocialsInstagram());

            statement.setString(19, user.toString());

            if (statement.executeUpdate() == 1)
                survey.setExisting(true);

        } catch (SQLException sql) {
            log.log(Level.SEVERE, sql.getMessage(), sql);
        }
    }
}
