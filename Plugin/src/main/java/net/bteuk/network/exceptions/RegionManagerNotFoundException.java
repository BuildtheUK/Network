package net.bteuk.network.exceptions;

/**
 * Exception that gets throws when a region manager can not be found.
 */
public class RegionManagerNotFoundException extends Exception {
    public RegionManagerNotFoundException(String error) {
        super(error);
    }
}
