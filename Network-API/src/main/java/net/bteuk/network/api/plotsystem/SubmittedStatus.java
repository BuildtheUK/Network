package net.bteuk.network.api.plotsystem;

import lombok.Getter;

import java.util.Objects;

/**
 * Enum for the status of a submitted plot as defined in the database.
 */
@Getter
public enum SubmittedStatus {
    SUBMITTED("submitted"),
    UNDER_REVIEW("under review"),
    AWAITING_VERIFICATION("awaiting verification"),
    UNDER_VERIFICATION("under verification");

    private final String databaseValue;

    SubmittedStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    /**
     * Get the {@link SubmittedStatus} from the database value.
     *
     * @param value the database value
     * @return the SubmittedStatus, or null if none match
     */
    public static SubmittedStatus fromDatabaseValue(String value) {
        for (SubmittedStatus status : SubmittedStatus.values()) {
            if (Objects.equals(status.databaseValue, value)) {
                return status;
            }
        }
        return null;
    }
}
