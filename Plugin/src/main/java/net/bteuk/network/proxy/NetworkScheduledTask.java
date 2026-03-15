package net.bteuk.network.proxy;

import org.btuk.proxy.core.scheduler.ScheduledTask;
import org.btuk.proxy.core.scheduler.TaskStatus;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class NetworkScheduledTask implements ScheduledTask {

    private JavaPlugin plugin;

    private BukkitTask task;

    /**
     * Constructor for a scheduled task.
     *
     * @param plugin the plugin that owns the task
     * @param task   the task to be scheduled
     */
    public NetworkScheduledTask(JavaPlugin plugin, BukkitTask task) {
        this.plugin = plugin;
        this.task = task;
    }

    /**
     * Constructor for a repeating task, these are tasks that run repeatedly.
     */
    public NetworkScheduledTask() {
        // Do nothing.
    }

    @Override
    public void cancel() {
        task.cancel();
    }

    @Override
    public TaskStatus getStatus() {
        if (task != null && plugin != null) {
            if (task.isCancelled()) {
                return TaskStatus.CANCELLED;
            } else if (plugin.getServer().getScheduler().getPendingTasks().contains(task)) {
                return TaskStatus.SCHEDULED;
            }
            return TaskStatus.FINISHED;
        } else {
            return TaskStatus.SCHEDULED;
        }
    }
}
