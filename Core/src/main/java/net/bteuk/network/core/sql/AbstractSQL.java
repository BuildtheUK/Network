package net.bteuk.network.core.sql;

import lombok.extern.java.Log;
import net.bteuk.network.api.SQLAPI;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

@Log
public abstract class AbstractSQL implements SQLAPI {

    private final DataSource dataSource;

    public AbstractSQL(DataSource datasource) {
        this.dataSource = datasource;
    }

    protected Connection conn() throws SQLException {
        return dataSource.getConnection();
    }

    public boolean hasRow(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            return results.next();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in hasRow: " + sql, e);
            return false;
        }
    }

    // Generic update statement, return true if successful.
    public boolean update(String sql, Object... args) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql)
        ) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            statement.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL update failed: " + sql, e);
            return false;
        }
    }

    // Generic update statement, return true if successful.
    public boolean update(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql)
        ) {

            statement.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL update failed: " + sql, e);
            return false;
        }
    }

    public boolean getBoolean(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            if (results.next()) {

                return results.getBoolean(1);
            } else {

                return false;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getBoolean: " + sql, e);
            return false;
        }
    }

    public int getInt(String sql, Object... args) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql)
        ) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getInt(1);
                } else {
                    return 0;
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getInt: " + sql, e);
            return 0;
        }
    }

    public int getInt(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            if (results.next()) {

                return results.getInt(1);
            } else {

                return 0;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getInt: " + sql, e);
            return 0;
        }
    }

    public double getDouble(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            if (results.next()) {

                return results.getDouble(1);
            } else {

                return 0;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getDouble: " + sql, e);
            return 0;
        }
    }

    public float getFloat(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            if (results.next()) {

                return results.getInt(1);
            } else {

                return 0;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getFloat: " + sql, e);
            return 0;
        }
    }

    public long getLong(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            if (results.next()) {

                return results.getLong(1);
            } else {

                return 0;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getLong: " + sql, e);
            return 0;
        }
    }

    public String getString(String sql, Object... args) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql)
        ) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            try (ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return results.getString(1);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getString: " + sql, e);
            return null;
        }
    }

    public String getString(String sql) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            if (results.next()) {

                return results.getString(1);
            } else {

                return null;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getString: " + sql, e);
            return null;
        }
    }

    public ArrayList<String> getStringList(String sql) {

        ArrayList<String> list = new ArrayList<>();

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            while (results.next()) {

                list.add(results.getString(1));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getStringList: " + sql, e);
            return null;
        }

        return list;
    }

    public ArrayList<Integer> getIntList(String sql) {

        ArrayList<Integer> list = new ArrayList<>();

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {

            while (results.next()) {

                list.add(results.getInt(1));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getIntList: " + sql, e);
            return null;
        }

        return list;
    }

    public Map<Integer, String> getIntStringMap(String sql, Object... args) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql)
        ) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            try (ResultSet results = statement.executeQuery()) {
                Map<Integer, String> map = new LinkedHashMap<>();
                while (results.next()) {
                    map.put(results.getInt(1), results.getString(2));
                }
                return map;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getIntStringMap: " + sql, e);
            return null;
        }
    }

    public Map<Integer, String> getIntStringMap(String sql) {

        Map<Integer, String> map = new HashMap<>();

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                map.put(results.getInt(1), results.getString(2));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An invalid sql query was attempted, " + sql, e);
        }
        return map;
    }

    public Map<String, Integer> getStringIntMap(String sql, Object... args) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql)
        ) {

            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }

            try (ResultSet results = statement.executeQuery()) {
                Map<String, Integer> map = new LinkedHashMap<>();
                while (results.next()) {
                    map.put(results.getString(1), results.getInt(2));
                }
                return map;
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "SQL query failed in getStringIntMap: " + sql, e);
            return null;
        }
    }

    public Map<String, Integer> getStringIntMap(String sql) {

        Map<String, Integer> map = new LinkedHashMap<>();

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                map.put(results.getString(1), results.getInt(2));
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An invalid sql query was attempted, " + sql, e);
        }
        return map;
    }
}
