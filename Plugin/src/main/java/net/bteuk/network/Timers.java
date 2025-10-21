package net.bteuk.network;

import lombok.extern.java.Log;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

@Log
public class Timers {

    // Plugin
    private final Network instance;
    // Users
    private final ArrayList<NetworkUser> users;

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

    public Timers(Network instance, GlobalSQL globalSQL, EventManager eventManager, Constants constants, Afk afk) {

        this.instance = instance;
        this.users = instance.getUsers();

        this.globalSQL = globalSQL;

        this.eventManager = eventManager;
        events = new ArrayList<>();

        this.constants = constants;

        this.afk = afk;

        // Minutes * 60 seconds * 1000 milliseconds
        afkTime = constants.afkTime() * 60L * 1000L;

        startTimers();
    }

    public void startTimers() {

        // 1-tick timer (50ms)
        instance.getTimerAPI().registerTimer(() -> {

            // Check for new server_events.
            if (globalSQL.hasRow("SELECT uuid FROM server_events WHERE server='" + constants.serverName() + "';")) {

                // If events are not empty, skip this iteration.
                // Additionally, isBusy needs to be false, implying that the server is not still running a previous
                // iteration.
                if (events.isEmpty() && !isBusy) {

                    isBusy = true;

                    // Get events for this server.
                    events = globalSQL.getEvents(constants.serverName(), events);

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
        }, 50L);

        // 1-second timer (1000ms)
        instance.getTimerAPI().registerTimer(() -> {

            // Get current time.
            long time = Time.currentTime();

            for (NetworkUser user : users) {

                // If navigator is enabled check if they have it in slot 9.
                if (user.isNavigatorEnabled()) {
                    slot9 = user.player.getInventory().getItem(8);

                    if (slot9 == null) {
                        user.player.getInventory().setItem(8, instance.getNavigatorItem());
                    } else if (!(slot9.equals(instance.getNavigatorItem()))) {
                        user.player.getInventory().setItem(8, instance.getNavigatorItem());
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
        }, 1000L);
    }
}