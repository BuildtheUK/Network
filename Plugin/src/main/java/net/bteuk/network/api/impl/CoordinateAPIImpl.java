package net.bteuk.network.api.impl;

import net.bteuk.network.api.CoordinateAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.Coordinate;

public class CoordinateAPIImpl implements CoordinateAPI {

    private final GlobalSQL globalSQL;

    public CoordinateAPIImpl(GlobalSQL globalSQL) {
        this.globalSQL = globalSQL;
    }

    /**
     * Add a coordinate to the database and return its id.
     * @param location the location to create a coordinate of
     * @return the coordinate id
     */
    @Override
    public int addCoordinate(NetworkLocation location) {
        return globalSQL.addCoordinate(location);
    }

    @Override
    public int copyCoordinate(int coordinateID) {
        Coordinate coordinate = globalSQL.getCoordinate(coordinateID);
        if (coordinate != null) {
            return globalSQL.addCoordinate(coordinate);
        }
        return -1;
    }

    @Override
    public void updateCoordinate(int coordinateID, NetworkLocation location) {
        globalSQL.updateCoordinate(coordinateID, location);
    }

    @Override
    public double getX(int coordinateID) {
        return 0;
    }

    @Override
    public double getZ(int coordinateID) {
        return 0;
    }

    @Override
    public NetworkLocation getLocation(int coordinateID) {
        return null;
    }
}
