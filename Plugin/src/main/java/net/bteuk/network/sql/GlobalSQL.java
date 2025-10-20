package net.bteuk.network.sql;

import lombok.extern.java.Log;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.building_counter.Building;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.sql.AbstractSQL;
import net.bteuk.network.lib.dto.DirectMessage;
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
import java.util.ArrayList;
import java.util.List;

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
            e.printStackTrace();
            return list;
        }

        // Try and delete all events for this server.
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("DELETE FROM server_events WHERE server='" + serverName + "';")
        ) {

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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

            sql.printStackTrace();
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

            sql.printStackTrace();
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

            sql.printStackTrace();
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
                buildings.add(new Building(results.getInt("building_id"), temp, results.getString("player_id"), results.getInt("coordinate_id")));
            }
            return buildings;
        } catch (SQLException sql) {
            sql.printStackTrace();
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

            sql.printStackTrace();
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

            sql.printStackTrace();
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
            sql.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return messages;
    }
}
