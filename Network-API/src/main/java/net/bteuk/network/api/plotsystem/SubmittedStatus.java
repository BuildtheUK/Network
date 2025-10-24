package net.bteuk.network.api.plotsystem;

import java.util.Objects;

/**
 * Enum for the status of a submitted plot as defined in the database.
 */
public enum SubmittedStatus {
    SUBMITTED("submitted"),
    UNDER_REVIEW("under review"),
    AWAITING_VERIFICATION("awaiting verification"),
    UNDER_VERIFICATION("under verification");

    public final String database_value;

    SubmittedStatus(String database_value) {
        this.database_value = database_value;
    }

    /**
     * Get the {@link SubmittedStatus} from the database value.
     *
     * @param value the database value
     * @return the SubmittedStatus, or null if none match
     */
    public static SubmittedStatus fromDatabaseValue(String value) {
        for (SubmittedStatus status : SubmittedStatus.values()) {
            if (Objects.equals(status.database_value, value)) {
                return status;
            }
        }
        return null;
    }
}
