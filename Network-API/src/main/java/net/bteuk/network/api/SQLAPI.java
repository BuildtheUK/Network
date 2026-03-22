package net.bteuk.network.api;

import java.util.HashMap;
import java.util.List;

public interface SQLAPI {

    boolean hasRow(String sql);

    int getInt(String sql, Object... args);

    int getInt(String sql);

    long getLong(String sql);

    String getString(String sql, Object... args);

    String getString(String sql);

    HashMap<String, Integer> getStringIntMap(String sql, Object... args);

    HashMap<String, Integer> getStringIntMap(String sql);

    HashMap<Integer, String> getIntStringMap(String sql, Object... args);

    HashMap<Integer, String> getIntStringMap(String sql);

    List<String> getStringList(String sql);

    List<Integer> getIntList(String sql);

}
