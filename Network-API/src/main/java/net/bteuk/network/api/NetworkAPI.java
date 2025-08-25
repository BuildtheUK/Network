package net.bteuk.network.api;

import net.bteuk.network.api.entity.ShutdownHook;

public interface NetworkAPI {

    ChatAPI getChat();

    PlotAPI getPlotAPI();

    SQLAPI getGlobalSQL();

    TimerAPI getTimerAPI();

    /**
     * Register a shutdown hook to run on server shutdown.
     *
     * @param hook the shutdown hook
     */
    void registerShutdownHook(ShutdownHook hook);
}
