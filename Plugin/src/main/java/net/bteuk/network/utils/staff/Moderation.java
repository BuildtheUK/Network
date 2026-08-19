package net.bteuk.network.utils.staff;

import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.core.Time;
import net.bteuk.network.exceptions.DurationFormatException;
import net.bteuk.network.exceptions.NotBannedException;
import net.bteuk.network.exceptions.NotMutedException;
import net.bteuk.network.socket.MessageSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.btuk.network.lib.dto.DirectMessage;
import org.btuk.network.lib.dto.ModerationEvent;
import org.btuk.network.lib.enums.ChatChannels;
import org.btuk.network.lib.enums.ModerationAction;
import org.btuk.network.lib.utils.ChatUtils;

/**
 * Utility class for moderation
 */
public class Moderation {

    private final Network instance;

    private final EventAPI eventAPI;
    
    private final MessageSender messageSender;

    public Moderation(Network instance, EventAPI eventAPI, MessageSender messageSender) {
        this.instance = instance;
        this.eventAPI = eventAPI;
        this.messageSender = messageSender;
    }

    // Ban the player.
    public void ban(String uuid, long end_time, String reason) throws NotBannedException {

        // Get time.
        long time = Time.currentTime();

        // If the player is already banned, end the old ban.
        if (isBanned(uuid)) {
            instance.getGlobalSQL()
                    .update("UPDATE moderation SET end_time=" + time + " WHERE uuid='" + uuid + "' AND end_time>" + time + " AND type='ban';");
        }
        instance.getGlobalSQL().update("INSERT INTO moderation(uuid,start_time,end_time,reason,type) " +
                "VALUES('" + uuid + "'," + time + "," + end_time + ",'" + reason + "','ban');");

        // If the player is currently online, ban them.
        kick(uuid, LegacyComponentSerializer.legacyAmpersand().serialize(getBannedComponent(uuid)));
    }

    // Mute the player.
    public void mute(String uuid, long end_time, String reason) throws NotMutedException {

        // Get time.
        long time = Time.currentTime();

        // If the player is already muted, end the old mute.
        if (isMuted(uuid)) {
            instance.getGlobalSQL()
                    .update("UPDATE moderation SET end_time=" + time + " WHERE uuid='" + uuid + "' AND end_time>" + time + " AND type='mute';");
        }
        instance.getGlobalSQL().update("INSERT INTO moderation(uuid,start_time,end_time,reason,type) " +
                "VALUES('" + uuid + "'," + time + "," + end_time + ",'" + reason + "','mute');");

        // Update Tab by sending a moderation event.
        Component mutedComponent = getMutedComponent(uuid);
        ModerationEvent moderationEvent = new ModerationEvent(ModerationAction.MUTE, null, uuid, end_time,
                mutedComponent);
        messageSender.sendSocketMessage(moderationEvent);

        // Notify the user.
        DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid, "server",
                mutedComponent, true);
        messageSender.sendSocketMessage(directMessage);
    }

    // Unban the player.
    public void unban(String uuid) {
        // Get time.
        long time = Time.currentTime();
        instance.getGlobalSQL()
                .update("UPDATE moderation SET end_time=" + time + " WHERE uuid='" + uuid + "' AND end_time>" + time + " AND type='ban';");
    }

    // Unmute the player.
    public void unmute(String uuid) {
        // Get time.
        long time = Time.currentTime();
        instance.getGlobalSQL()
                .update("UPDATE moderation SET end_time=" + time + " WHERE uuid='" + uuid + "' AND end_time>" + time + " AND type='mute';");

        // Update Tab by sending a moderation event.
        ModerationEvent moderationEvent = new ModerationEvent(ModerationAction.UNMUTE, null, uuid, 0L, null);
        messageSender.sendSocketMessage(moderationEvent);
    }

    // Kick the player.
    public void kick(String uuid, String reason) {
        // Kick them with the reason, if online.
        instance.getOnlineUserByUuid(uuid).ifPresent(onlineUser -> eventAPI.createEvent(uuid, onlineUser.getServer(), "kick", reason));
    }

    /**
     * Check whether a player is banned.
     *
     * @param uuid the uuid of the player
     * @return true if the player is currently banned, false if not
     */
    public boolean isBanned(String uuid) {
        return (instance.getGlobalSQL().hasRow("SELECT uuid FROM moderation WHERE uuid='" + uuid + "' " +
                "AND end_time>" + Time.currentTime() + " AND type='ban';"));
    }

    // If the player is currently muted, return true.
    public boolean isMuted(String uuid) {
        return (instance.getGlobalSQL().hasRow("SELECT uuid FROM moderation WHERE uuid='" + uuid + "' " +
                "AND end_time>" + Time.currentTime() + " AND type='mute';"));
    }

    // Get reason why player is banned.
    public String getBannedReason(String uuid) {
        return (instance.getGlobalSQL().getString("SELECT reason FROM moderation WHERE uuid='" + uuid +
                "' AND end_time>" + Time.currentTime() + " AND type='ban';"));
    }

    // Get reason why player is muted.
    public String getMutedReason(String uuid) {
        return (instance.getGlobalSQL().getString("SELECT reason FROM moderation WHERE uuid='" + uuid +
                "' AND end_time>" + Time.currentTime() + " AND type='mute';"));
    }

    // Get duration of ban.
    public String getBanDuration(String uuid) {
        long time =
                instance.getGlobalSQL().getLong("SELECT end_time FROM moderation WHERE uuid='" + uuid +
                        "' AND end_time>" + Time.currentTime() + " AND type='ban';");
        return Time.getDateTime(time);
    }

    // Get duration of mute.
    public String getMuteDuration(String uuid) {
        long time =
                instance.getGlobalSQL().getLong("SELECT end_time FROM moderation WHERE uuid='" + uuid +
                        "' AND end_time>" + Time.currentTime() + " AND type='mute';");
        return Time.getDateTime(time);
    }

    /**
     * Get Component for banned player to display.
     * This assumes that the player is banned, else this will throw an exception.
     *
     * @param uuid the uuid of the banned player
     * @return the component of the banned message with reason and duration
     * @throws NotBannedException if the player is not banned
     */
    public Component getBannedComponent(String uuid) throws NotBannedException {
        if (isBanned(uuid)) {
            return Component.text("You have been banned for ", NamedTextColor.RED)
                    .append(Component.text(getBannedReason(uuid), NamedTextColor.DARK_RED))
                    .append(Component.text(" until ", NamedTextColor.RED))
                    .append(Component.text(getBanDuration(uuid), NamedTextColor.DARK_RED));
        } else {
            throw new NotBannedException("The user with uuid " + uuid + " is not banned.");
        }
    }

    /**
     * Get Component for muted player to display.
     * This assumes that the player is muted, else this will throw an exception.
     *
     * @param uuid the uuid of the muted player
     * @return the component of the muted message with reason and duration
     * @throws NotMutedException if the player is not muted
     */
    public Component getMutedComponent(String uuid) throws NotMutedException {
        if (isMuted(uuid)) {
            return ChatUtils.error("You have been muted for ")
                    .append(Component.text(getMutedReason(uuid), NamedTextColor.DARK_RED))
                    .append(ChatUtils.error(" until "))
                    .append(Component.text(getMuteDuration(uuid), NamedTextColor.DARK_RED));
        } else {
            throw new NotMutedException("The user with uuid " + uuid + " is not muted.");
        }
    }

    /**
     * Convert a string to a long time for the ban duration.
     *
     * @param formattedInput input string in ymdh format
     * @return duration in milliseconds after converting the input string
     * @throws DurationFormatException if the input string is not formatted correctly
     */
    public long getDuration(String formattedInput) throws DurationFormatException {

        if (formattedInput == null) {
            throw new NullPointerException();
        }

        // Add random letter at the end of the duration string, so it'll always split into 2 parts.
        String sDuration = formattedInput + "q";

        // Check for valid duration.
        // ymdh format (year, month, day, hour)
        String[] duration;
        long time = 0;

        // Check years
        duration = sDuration.split("y");

        if (duration.length == 2) {
            try {

                int years = Integer.parseInt(duration[0]);

                // Convert years to milliseconds and add to time.
                // We're assuming a year is 365 days.
                time += years * 365 * 24 * 60 * 60 * 1000L;

                // Remove the first part from the string as we've already converted it.
                sDuration = duration[1];
            } catch (NumberFormatException e) {
                throw new DurationFormatException("Duration must be in ymdh format, for example 1y6m, which is 1 year" +
                        " and 6 months or 2d12h is 2 days and 12 hours.");
            }
        }

        // Check months
        duration = sDuration.split("m");

        if (duration.length == 2) {
            try {

                int months = Integer.parseInt(duration[0]);

                // Convert months to milliseconds and add to time.
                // We're assuming a month is 30 days.
                time += months * 30 * 24 * 60 * 60 * 1000L;

                // Remove the first part from the string as we've already converted it.
                sDuration = duration[1];
            } catch (NumberFormatException e) {
                throw new DurationFormatException("Duration must be in ymdh format, for example 1y6m, which is 1 year" +
                        " and 6 months or 2d12h is 2 days and 12 hours.");
            }
        }

        // Check days
        duration = sDuration.split("d");

        if (duration.length == 2) {
            try {

                int days = Integer.parseInt(duration[0]);

                // Convert days to milliseconds and add to time.
                time += days * 24 * 60 * 60 * 1000L;

                // Remove the first part from the string as we've already converted it.
                sDuration = duration[1];
            } catch (NumberFormatException e) {
                throw new DurationFormatException("Duration must be in ymdh format, for example 1y6m, which is 1 year" +
                        " and 6 months or 2d12h is 2 days and 12 hours.");
            }
        }

        // Check hours
        duration = sDuration.split("h");

        if (duration.length == 2) {
            try {

                int hours = Integer.parseInt(duration[0]);

                // Convert hours to milliseconds and add to time.
                time += hours * 60 * 60 * 1000L;

                // Remove the first part from the string as we've already converted it.
                sDuration = duration[1];
            } catch (NumberFormatException e) {
                throw new DurationFormatException("Duration must be in ymdh format, for example 1y6m, which is 1 year" +
                        " and 6 months or 2d12h is 2 days and 12 hours.");
            }
        }

        // If the time is 0, or the string does not end with just the character q, then the format was not correct.
        if (time == 0 || !sDuration.equals("q")) {
            throw new DurationFormatException("Duration must be in ymdh format, for example 1y6m, which is 1 year and" +
                    " 6 months or 2d12h is 2 days and 12 hours.");
        } else {
            return time;
        }
    }

    public Component kickPlayer(String name, String uuid, String reason) {
        kick(uuid, reason);
        return (ChatUtils.success("Kicked ")
                .append(Component.text(name, NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" for reason: "))
                .append(Component.text(reason, NamedTextColor.DARK_AQUA)));
    }

    /**
     * Ban the player and return the feedback so the executor can be notified of success/failure.
     *
     * @param name     Name of the player to ban.
     * @param uuid     Uuid of the player to ban.
     * @param end_time Time for the ban to end in milliseconds.
     * @param reason   Reason for banning the player.
     * @return The Component to display to the executor.
     */
    public Component banPlayer(String name, String uuid, long end_time, String reason) {
        try {
            ban(uuid, end_time, reason);
        } catch (NotBannedException e) {
            return ChatUtils.error("An error occurred while banning this player, please contact an admin for support.");
        }

        return ChatUtils.success("Banned ")
                .append(Component.text(name, NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" until "))
                .append(Component.text(Time.getDateTime(end_time), NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" for reason: "))
                .append(Component.text(reason, NamedTextColor.DARK_AQUA));
    }

    /**
     * Mute the player and return the feedback so the executor can be notified of success/failure.
     *
     * @param name     Name of the player to mute.
     * @param uuid     Uuid of the player to mute.
     * @param end_time Time for the mute to end in milliseconds.
     * @param reason   Reason for muting the player.
     * @return The Component to display to the executor.
     */
    public Component mutePlayer(String name, String uuid, long end_time, String reason) {
        try {
            mute(uuid, end_time, reason);
        } catch (NotMutedException e) {
            return ChatUtils.error("An error occurred while muting this player, please contact an admin for support.");
        }

        return ChatUtils.success("Muted ")
                .append(Component.text(name, NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" until "))
                .append(Component.text(Time.getDateTime(end_time), NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" for reason: "))
                .append(Component.text(reason, NamedTextColor.DARK_AQUA));
    }

    /**
     * Unban the player and return the feedback so the executor can be notified of success/failure.
     *
     * @param name Name of the banned player.
     * @param uuid Uuid of the banned player.
     * @return The Component to display to the executor.
     */
    public Component unbanPlayer(String name, String uuid) {

        // Check if the player is currently banned.
        if (isBanned(uuid)) {

            // Unban the player.
            unban(uuid);

            // Send feedback.
            return (ChatUtils.success("Unbanned ")
                    .append(Component.text(name, NamedTextColor.DARK_AQUA)));
        } else {
            return (ChatUtils.error(name + " is not currently banned."));
        }
    }

    /**
     * Unmute the player and return the feedback so the executor can be notified of success/failure.
     *
     * @param name Name of the muted player.
     * @param uuid Uuid of the muted player.
     * @return The Component to display to the executor.
     */
    public Component unmutePlayer(String name, String uuid) {

        // Check if the player is currently muted.
        if (isMuted(uuid)) {

            // Unmute the player.
            unmute(uuid);

            // Send feedback.
            return (ChatUtils.success("Unmuted ")
                    .append(Component.text(name, NamedTextColor.DARK_AQUA)));
        } else {
            return (ChatUtils.error(name + " is not currently muted."));
        }
    }
}
