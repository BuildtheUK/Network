package net.bteuk.network.exceptions;

/**
 * Error that is thrown when there is a generic invalid format.
 */
public class InvalidFormatException extends Exception {
    public InvalidFormatException(String error) {
        super(error);
    }
}
