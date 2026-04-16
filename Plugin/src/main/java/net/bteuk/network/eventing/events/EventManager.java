package net.bteuk.network.eventing.events;

import lombok.extern.java.Log;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.entity.Event;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.core.Constants;
import net.bteuk.network.sql.GlobalSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.HashMap;

@Log
public class EventManager implements EventAPI, Event {

    private final HashMap<String, Event> events = new HashMap<>();
    private final GlobalSQL globalSQL;
    private final Constants constants;

    private Back back;

    public EventManager(GlobalSQL globalSQL, Constants constants) {
        this.globalSQL = globalSQL;
        this.constants = constants;
    }

    public void registerBack(Back back) {
        if (this.back == null) {
            this.back = back;
            log.info("Back function registered in EventManager.");
        }
    }

    public void registerEvent(String name, Event event) {
        events.put(name, event);
    }

    public void createJoinEvent(String uuid, String event) {
        globalSQL.update(
                "INSERT INTO join_events(uuid,event) VALUES(?,?) ON DUPLICATE KEY UPDATE event=?;", uuid, event, event);
    }

    public void createJoinEvent(String uuid, String event, String message) {
        globalSQL.update(
                "INSERT INTO join_events(uuid,event,message) VALUES(?,?,?) ON DUPLICATE KEY UPDATE event=?, message=?;", uuid, event, message, event, message);
    }

    public void createJoinEvent(String uuid, String event, Component message) {
        String messageString = PlainTextComponentSerializer.plainText().serialize(message);
        globalSQL.update(
                "INSERT INTO join_events(uuid,event,message) VALUES(?,?,?) ON DUPLICATE KEY UPDATE event=?, message=?;", uuid, event, messageString, event, messageString);
    }

    /**
     * Creates an event with the following input parameters:
     *
     * @param uuid   the uuid of the player to which the event should apply
     * @param server the server name where the event should occur
     * @param event  the event arguments in String format
     */
    public void createEvent(String uuid, String server, String event) {
        if (uuid == null) {
            globalSQL.update("INSERT INTO server_events(server,event) VALUES(?,?);", server, event);
        } else {
            globalSQL.update("INSERT INTO server_events(uuid,server,event) VALUES(?,?,?);", uuid, server, event);
        }
    }

    /**
     * Creates an event with the following input parameters:
     *
     * @param uuid    the uuid of the player to which the event should apply
     * @param server  the server name where the event should occur
     * @param event   the event arguments in String format
     * @param message message to be sent to the player on success
     */
    public void createEvent(String uuid, String server, String event, String message) {
        if (uuid == null) {
            globalSQL.update("INSERT INTO server_events(server,event,message) VALUES(?,?,?);", server, event, message);
        } else {
            globalSQL.update(
                    "INSERT INTO server_events(uuid,server,event,message) VALUES(?,?,?,?);", uuid, server, event, message);
        }
    }

    /**
     * Creates an event with the following input parameters:
     *
     * @param uuid    the uuid of the player to which the event should apply
     * @param server  the server name where the event should occur
     * @param event   the event arguments in String format
     * @param message message to be sent to the player on success
     */
    public void createEvent(String uuid, String server, String event, Component message) {
        String messageString = PlainTextComponentSerializer.plainText().serialize(message);
        if (uuid == null) {
            globalSQL.update("INSERT INTO server_events(server,event,message) VALUES(?,?,?);", server, event, messageString);
        } else {
            globalSQL.update(
                    "INSERT INTO server_events(uuid,server,event,message) VALUES(?,?,?,?);", uuid, server, event, messageString);
        }
    }

    public void createTeleportEvent(boolean join, String uuid, String event, NetworkLocation previousLocation) {

        back.setPreviousCoordinate(uuid, previousLocation);

        // Create event
        if (join) {
            createJoinEvent(uuid, event);
        } else {
            createEvent(uuid, constants.serverName(), event);
        }
    }

    public void createTeleportEvent(boolean join, String uuid, String event, String message, NetworkLocation previousLocation) {

        back.setPreviousCoordinate(uuid, previousLocation);

        // Create event
        if (join) {
            createJoinEvent(uuid, event, message);
        } else {
            createEvent(uuid, constants.serverName(), event, message);
        }
    }

    public void createTeleportEvent(boolean join, String uuid, String event, Component message, NetworkLocation previousLocation) {

        String messageString = PlainTextComponentSerializer.plainText().serialize(message);
        back.setPreviousCoordinate(uuid, previousLocation);

        // Create event
        if (join) {
            createJoinEvent(uuid, event, messageString);
        } else {
            createEvent(uuid, constants.serverName(), event, messageString);
        }
    }

    /**
     * General implementation of an event, uses a switch expression to run the actual event.
     *
     * @param uuid    the uuid of the player to whom this event applies
     * @param event   arguments of the event
     * @param message optional message to send to the player after the event has executed successfully
     */
    @Override
    public void event(String uuid, String[] event, String message) {
        log.info("Event " + event[0] + " triggered by " + uuid + " with arguments " + String.join(", ", event));
        Event eventType = events.get(event[0]);
        if (eventType == null) {
            log.warning("Event " + event[0] + " is not registered.");
        } else {
            eventType.event(uuid, event, message);
        }
    }
}
