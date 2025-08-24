package net.bteuk.network;

import lombok.extern.java.Log;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.regions.Inactivity;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

import static net.bteuk.network.utils.NetworkConfig.CONFIG;

@Log
public class Timers {

    // Region Inactivity
    public final long inactivity;
    // Plugin
    private final Network instance;
    // Users
    private final ArrayList<NetworkUser> users;
    // Timers
    private final ArrayList<Integer> timers;
    // SQL
    private final GlobalSQL globalSQL;

    private final Afk afk;

    // Afk time
    private final long afkTime;
    // Event manager
    private final EventManager eventManager;

    private final Constants constants;

    // Server events
    private ArrayList<String[]> events;
    private boolean isBusy;
    // Navigator Check
    private ItemStack slot9;
    private ArrayList<Inactivity> inactive_owners;
    private String uuid;

    public Timers(Network instance, GlobalSQL globalSQL, EventManager eventManager, Constants constants, Afk afk) {

        this.instance = instance;
        this.users = instance.getUsers();

        this.globalSQL = globalSQL;

        this.timers = new ArrayList<>();

        this.eventManager = eventManager;
        events = new ArrayList<>();

        this.constants = constants;

        // days * 24 hours * 60 minutes * 60 seconds * 1000 milliseconds
        inactivity = CONFIG.getInt("region_inactivity") * 24L * 60L * 60L * 1000L;
        inactive_owners = new ArrayList<>();

        this.afk = afk;

        // Minutes * 60 seconds * 1000 milliseconds
        afkTime = CONFIG.getInt("afk") * 60L * 1000L;
    }

    public void startTimers() {

        // 1 tick timer.
        timers.add(instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, () -> {

            // Check for new server_events.
            if (globalSQL.hasRow("SELECT uuid FROM server_events WHERE server='" + constants.serverName() + "' AND type='network" +
                    "';")) {

                // If events is not empty, skip this iteration.
                // Additionally isBusy needs to be false, implying that the server is not still running a previous
                // iteration.
                if (events.isEmpty() && !isBusy) {

                    isBusy = true;

                    // Get events for this server.
                    events = globalSQL.getEvents(constants.serverName(), "network", events);

                    for (String[] event : events) {

                        // Deal with events here.
                        log.info("Event: " + event[1]);

                        // Split the event by word.
                        String[] aEvent = event[1].split(" ");

                        // Send the event to the event handler.
                        eventManager.event(event[0], aEvent, event[2]);
                    }

                    // Clear events when done.
                    events.clear();
                    isBusy = false;
                }
            }
        }, 0L, 1L));

        // 1-second timer.
        timers.add(instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, () -> {

            // Get current time.
            long time = Time.currentTime();

            for (NetworkUser user : users) {

                // If navigator is enabled check if they have it in slot 9.
                if (user.isNavigatorEnabled()) {
                    slot9 = user.player.getInventory().getItem(8);

                    if (slot9 == null) {
                        user.player.getInventory().setItem(8, instance.navigator);
                    } else if (!(slot9.equals(instance.navigator))) {
                        user.player.getInventory().setItem(8, instance.navigator);
                    }
                }

                // Check if the player is afk.
                if (user.last_movement < (time - afkTime) && !user.isAfk()) {

                    // Set player as AFK
                    user.setAfk(true);

                    // Send message to chat and discord.
                    afk.updateAfkStatus(user, true);
                }
            }
        }, 0L, 20L));

        // 1-minute timer.
        // TODO: Move to region code.
        if (constants.regionsEnabled()) {
            timers.add(instance.getServer().getScheduler().scheduleSyncRepeatingTask(instance, () -> {

                // Check for inactive owners.
                // If the region has members then make another member the new owner,
                // If the region has no members then set it inactive.

                inactive_owners.clear();
                inactive_owners = instance.regionSQL.getInactives("SELECT rm.region,rm.uuid FROM region_members AS rm" +
                        " INNER JOIN regions AS r ON rm.region=r.region WHERE rm.is_owner=1 AND rm.last_enter<" + (Time.currentTime() - inactivity) + " AND r.status <> " +
                        "'inactive';");
                for (Inactivity inactive : inactive_owners) {

                    // Check if there is another member in this region, they must be active.
                    if (inactive.region.hasActiveMember(Time.currentTime() - inactivity)) {

                        // Get most recent member.
                        uuid = inactive.region.getRecentMember();

                        // Make the previous owner a member.
                        inactive.region.makeMember();

                        // Give the new player ownership.
                        inactive.region.makeOwner(uuid);

                        // Update any requests to take into account the new region owner.
                        inactive.region.updateRequests();
                    } else {

                        // Set region as inactive.
                        inactive.region.setInactive();
                    }
                }

            }, 0L, 1200L));
        }
    }

    public void close() {

        // Cancel all timers.
        for (int timer : timers) {
            Bukkit.getScheduler().cancelTask(timer);
        }
    }
}