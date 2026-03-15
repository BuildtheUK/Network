package net.bteuk.network.api;

import java.util.List;

public interface SQLAPI {

    boolean hasRow(String sql);

    int getInt(String sql);

    long getLong(String sql);

    String getString(String sql);

    List<String> getStringList(String sql);

    List<Integer> getIntList(String sql);

}
