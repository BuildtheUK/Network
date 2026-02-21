package net.bteuk.network.api.entity;

/**
 * Hook to run on server shutdown, order of execution is not guaranteed.
 */
@FunctionalInterface
public interface ShutdownHook {
    /**
     * Runs on server shutdown.
     */
    void shutdown();
}