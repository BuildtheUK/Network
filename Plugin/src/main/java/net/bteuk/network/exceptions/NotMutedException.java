package net.bteuk.network.exceptions;

/**
 * Exception that gets throws when attempting to access information of a muted player but, they are not actually muted.
 * This would otherwise cause the functionality to break, this exception therefore explains the reason why the
 * function failed.
 */
public class NotMutedException extends Exception {
    public NotMutedException(String error) {
        super(error);
    }
}
