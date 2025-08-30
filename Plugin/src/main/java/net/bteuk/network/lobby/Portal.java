package net.bteuk.network.lobby;

import net.bteuk.network.eventing.events.EventManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Portal {

    private final EventManager eventManager;
    private final double x1, y1, z1, x2, y2, z2;
    private final String[] events;

    public Portal(EventManager eventManager, int x1, int y1, int z1, int x2, int y2, int z2, String[] events) {
        this.eventManager = eventManager;

        // Check if the min/max area configured correctly.
        // Else which them out.
        // Increase the max values by 1, so they are exactly the edge of the block.
        if (x1 <= x2) {
            this.x1 = x1;
            this.x2 = x2 + 1;
        } else {
            this.x1 = x2;
            this.x2 = x1 + 1;
        }

        if (y1 <= y2) {
            this.y1 = y1;
            this.y2 = y2 + 1;
        } else {
            this.y1 = y2;
            this.y2 = y1 + 1;
        }

        if (z1 <= z2) {
            this.z1 = z1;
            this.z2 = z2 + 1;
        } else {
            this.z1 = z2;
            this.z2 = z1 + 1;
        }

        this.events = events;
    }

    // Check if the location parameter is located inside the portal.
    public boolean in(Location l) {
        return x1 <= l.getX() && x2 >= l.getX() && y1 <= l.getY() && y2 >= l.getY() && z1 <= l.getZ() && z2 >= l.getZ();
    }

    // Runs the portal events for a specific player.
    // Leverages existing event infrastructure to run the events.
    // If the event is a command, execute that instead.
    public void event(Player p) {
        for (String event : events) {
            if (event.startsWith("/")) {

                // Send command by using chat.
                p.chat(event);
            } else {
                eventManager.event(p.getUniqueId().toString(), event.split(" "), null);
            }
        }
    }
}
