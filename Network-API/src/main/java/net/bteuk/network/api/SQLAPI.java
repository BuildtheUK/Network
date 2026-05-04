package net.bteuk.network.api;

import java.util.List;
import java.util.Map;

public interface SQLAPI {

    boolean hasRow(String sql, Object... args);

    boolean hasRow(String sql);

    boolean getBoolean(String sql, Object... args);

    boolean getBoolean(String sql);

    int getInt(String sql, Object... args);

    int getInt(String sql);

    double getDouble(String sql, Object... args);

    double getDouble(String sql);

    float getFloat(String sql, Object... args);

    float getFloat(String sql);

    long getLong(String sql, Object... args);

    long getLong(String sql);

    String getString(String sql, Object... args);

    String getString(String sql);

    Map<String, Integer> getStringIntMap(String sql, Object... args);

    Map<String, Integer> getStringIntMap(String sql);

    Map<Integer, String> getIntStringMap(String sql, Object... args);

    Map<Integer, String> getIntStringMap(String sql);

    List<String> getStringList(String sql, Object... args);

    List<String> getStringList(String sql);

    List<Integer> getIntList(String sql, Object... args);

    List<Integer> getIntList(String sql);

}
