package net.bteuk.network.api.plotsystem;

import java.util.Objects;

/**
 * Enum for the status of a plot as defined in the database.
 */
public enum PlotStatus {
    UNCLAIMED("unclaimed"),
    CLAIMED("claimed"),
    SUBMITTED("submitted"),
    COMPLETED("completed"),
    DELETED("deleted");

    public final String database_value;

    PlotStatus(String database_value) {
        this.database_value = database_value;
    }

    /**
     * Get the {@link PlotStatus} from the database value.
     *
     * @param value the database value
     * @return the PlotStatus, or null if none match
     */
    public static PlotStatus fromDatabaseValue(String value) {
        for (PlotStatus status : PlotStatus.values()) {
            if (Objects.equals(status.database_value, value)) {
                return status;
            }
        }
        return null;
    }
}
