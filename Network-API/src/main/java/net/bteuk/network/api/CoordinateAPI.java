package net.bteuk.network.api;

import net.bteuk.network.api.entity.NetworkLocation;

public interface CoordinateAPI {

    int addCoordinate(NetworkLocation location);

    int copyCoordinate(int coordinateID);

    void updateCoordinate(int coordinateID, NetworkLocation location);

    double getX(int coordinateID);

    double getZ(int coordinateID);

    NetworkLocation getLocation(int coordinateID);
}
