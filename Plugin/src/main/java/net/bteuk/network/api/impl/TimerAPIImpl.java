package net.bteuk.network.api.impl;

import net.bteuk.network.Network;
import net.bteuk.network.api.TimerAPI;
import net.bteuk.network.api.entity.ShutdownHook;

import java.util.HashMap;
import java.util.Map;

public class TimerAPIImpl implements TimerAPI, ShutdownHook {

    private final Map<Integer, Runnable> timers = new HashMap<>();

    private final Network instance;

    public TimerAPIImpl(Network instance) {
        this.instance = instance;
        instance.registerShutdownHook(this);
    }

    @Override
    public int registerTimer(Runnable runnable, long intervalMillis, long delay) {
        long serverTickInterval = Math.round(intervalMillis / 50.0);
        long serverTickDelay = Math.round(delay / 50.0);
        int id = instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, runnable, serverTickDelay, serverTickInterval);
        timers.put(id, runnable);
        return id;
    }

    @Override
    public void cancelTimer(int timerId) {
        if (timers.containsKey(timerId)) {
            instance.getServer().getScheduler().cancelTask(timerId);
            timers.remove(timerId);
        }
    }

    @Override
    public void shutdown() {
        timers.forEach((id, runnable) -> instance.getServer().getScheduler().cancelTask(id));
    }
}
