package net.bteuk.network.exceptions;

/**
 * Exception that gets throws when a world can not be found.
 */
public class WorldNotFoundException extends Exception {
    public WorldNotFoundException(String error) {
        super(error);
    }
}
