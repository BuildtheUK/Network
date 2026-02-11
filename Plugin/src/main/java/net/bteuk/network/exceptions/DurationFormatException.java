package net.bteuk.network.exceptions;

/**
 * Error that should be thrown when a string in ymdh format is inputted incorrectly.
 * A string in ymdh indicates a time of x years, x months, x days and x hours, any of those can be excluded, as long
 * as at least 1 is used.
 * <p>
 * Example:
 * 10y5d implies 10 years and 5 days in duration.
 */
public class DurationFormatException extends Exception {
    public DurationFormatException(String error) {
        super(error);
    }
}
