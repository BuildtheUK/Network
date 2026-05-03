package net.bteuk.network.api;

/**
 * Timer API allows the registering of timers.
 */
public interface TimerAPI {

    /**
     * Registers a timer at a specific interval.
     * The timer is run synchronously on the server thread, thus if the server is running below a tick rate of 20, the interval will be longer than the given time.
     *
     * @param runnable       the code to run on each interval
     * @param intervalMillis the interval
     * @return the timer id
     */
    default int registerTimer(Runnable runnable, long intervalMillis) {
        return registerTimer(runnable, intervalMillis, 0);
    }

    /**
     * Registers a timer at a specific interval.
     * The timer is run synchronously on the server thread, thus if the server is running below a tick rate of 20, the interval will be longer than the given time.
     *
     * @param runnable       the code to run on each interval
     * @param intervalMillis the interval
     * @param delay          the delay until first execution
     * @return the timer id
     */
    int registerTimer(Runnable runnable, long intervalMillis, long delay);

    /**
     * Registers a timer at a specific interval.
     * The timer is run asynchronously, thus it will not block the main server thread.
     *
     * @param runnable       the code to run on each interval
     * @param intervalMillis the interval
     * @return the timer id
     */
    default int registerAsyncTimer(Runnable runnable, long intervalMillis) {
        return registerAsyncTimer(runnable, intervalMillis, 0);
    }

    /**
     * Registers a timer at a specific interval.
     * The timer is run asynchronously, thus it will not block the main server thread.
     *
     * @param runnable       the code to run on each interval
     * @param intervalMillis the interval
     * @param delay          the delay until first execution
     * @return the timer id
     */
    int registerAsyncTimer(Runnable runnable, long intervalMillis, long delay);

    void cancelTimer(int timerId);
}
