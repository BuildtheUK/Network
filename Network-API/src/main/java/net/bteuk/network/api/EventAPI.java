package net.bteuk.network.api;

import net.bteuk.network.api.entity.Event;
import net.bteuk.network.api.entity.NetworkLocation;
import net.kyori.adventure.text.Component;

public interface EventAPI {

    /**
     * Registers an {@link Event} to the Event manager.
     *
     * @param name the name of the event
     * @param event the event
     */
    void registerEvent(String name, Event event);

    void createJoinEvent(String uuid, String type, String event);

    void createJoinEvent(String uuid, String type, String event, String message);

    void createJoinEvent(String uuid, String type, String event, Component message);

    /**
     * Creates an event with the following input parameters:
     *
     * @param uuid   the uuid of the player to which the event should apply
     * @param type   the type of event, this means the plugin which should run this event (network, plotsystem or a
     *               custom implementation)
     * @param server the server name where the event should occur
     * @param event  the event arguments in String format
     */
    void createEvent(String uuid, String type, String server, String event);

    /**
     * Creates an event with the following input parameters:
     *
     * @param uuid    the uuid of the player to which the event should apply
     * @param type    the type of event, this means the plugin which should run this event (network, plotsystem or a
     *                custom implementation)
     * @param server  the server name where the event should occur
     * @param event   the event arguments in String format
     * @param message message to be sent to the player on success
     */
    void createEvent(String uuid, String type, String server, String event, String message);

    /**
     * Creates an event with the following input parameters:
     *
     * @param uuid    the uuid of the player to which the event should apply
     * @param type    the type of event, this means the plugin which should run this event (network, plotsystem or a
     *                custom implementation)
     * @param server  the server name where the event should occur
     * @param event   the event arguments in String format
     * @param message message to be sent to the player on success
     */
    void createEvent(String uuid, String type, String server, String event, Component message);

    void createTeleportEvent(boolean join, String uuid, String type, String event, NetworkLocation previousLocation);

    void createTeleportEvent(boolean join, String uuid, String type, String event, String message, NetworkLocation previousLocation);

    void createTeleportEvent(boolean join, String uuid, String type, String event, Component message, NetworkLocation previousLocation);
}
