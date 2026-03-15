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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            return false;
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            return 0;
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
            e.printStackTrace();
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
            e.printStackTrace();
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
            e.printStackTrace();
            return null;
        }

        return list;
    }

    public HashMap<Integer, String> getIntStringMap(String sql) {

        HashMap<Integer, String> map = new HashMap<>();

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                map.put(results.getInt(1), results.getString(2));
            }
        } catch (SQLException e) {
            log.severe("An invalid sql query was attempted, " + sql);
        }
        return map;
    }

    public HashMap<String, Integer> getStringIntMap(String sql) {

        HashMap<String, Integer> map = new LinkedHashMap<>();

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(sql); ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                map.put(results.getString(1), results.getInt(2));
            }
        } catch (SQLException e) {
            log.severe("An invalid sql query was attempted, " + sql);
        }
        return map;
    }
}
