package net.bteuk.network.commands.navigation;

import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.sql.GlobalSQL;

public class PreviousLocationTracker {

    private final GlobalSQL globalSQL;

    public PreviousLocationTracker(GlobalSQL globalSQL) {
        this.globalSQL = globalSQL;
    }

    // Sets the location as the previous location in the database.
    public void setPreviousCoordinate(String uuid, NetworkLocation location) {

        // Set previous location for /back.
        int coordinateID = globalSQL.getInt("SELECT previous_coordinate FROM player_data WHERE uuid=?;", uuid);
        if (coordinateID == 0) {

            // No coordinate exists, create new.
            coordinateID = globalSQL.addCoordinate(location);

            // Set coordinate id in player data.
            globalSQL.update("UPDATE player_data SET previous_coordinate=? WHERE uuid=?;", coordinateID, uuid);
        } else {

            // Update existing coordinate.
            globalSQL.updateCoordinate(coordinateID, location);
        }
    }
}
