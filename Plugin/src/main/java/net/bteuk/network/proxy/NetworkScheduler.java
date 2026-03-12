package net.bteuk.network.proxy;

import org.btuk.proxy.core.scheduler.ScheduledTask;
import org.btuk.proxy.core.scheduler.Scheduler;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class NetworkScheduler implements Scheduler {
    
    private final JavaPlugin plugin;
    
    public NetworkScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public ScheduledTask createDelayedTask(Runnable runnable, long delay, TimeUnit timeUnit) {
        return new NetworkScheduledTask(plugin, plugin.getServer().getScheduler().runTaskLater(plugin, runnable, convertToTicks(delay, timeUnit)));
    }

    @Override
    public ScheduledTask createRepeatingTask(Runnable runnable, long delay, long period, TimeUnit timeUnit) {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, runnable, convertToTicks(delay, timeUnit), convertToTicks(period, timeUnit));
        return new NetworkScheduledTask();
    }
    
    private static long convertToTicks(long time, TimeUnit unit) {
        return TimeUnit.MILLISECONDS.convert(time, unit) / 50;
    }
}
