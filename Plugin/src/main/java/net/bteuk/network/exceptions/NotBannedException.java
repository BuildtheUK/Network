package net.bteuk.network.exceptions;

/**
 * Exception that gets throws when attempting to access information of a banned player but, they are not actually
 * banned.
 * This would otherwise cause the functionality to break, this exception therefore explains the reason why the
 * function failed.
 */
public class NotBannedException extends Exception {
    public NotBannedException(String error) {
        super(error);
    }
}
