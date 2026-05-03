package net.bteuk.network.api;

import java.util.List;
import java.util.Map;

public interface SQLAPI {

    boolean hasRow(String sql);

    int getInt(String sql, Object... args);

    int getInt(String sql);

    long getLong(String sql);

    String getString(String sql, Object... args);

    String getString(String sql);

    Map<String, Integer> getStringIntMap(String sql, Object... args);

    Map<String, Integer> getStringIntMap(String sql);

    Map<Integer, String> getIntStringMap(String sql, Object... args);

    Map<Integer, String> getIntStringMap(String sql);

    Map<Integer, Integer> getIntIntMap(String sql);

    Map<String, String> getStringStringMap(String sql);

    List<String> getStringList(String sql);

    List<Integer> getIntList(String sql);
}
