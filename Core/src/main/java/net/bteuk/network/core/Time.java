package net.bteuk.network.core;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Time {

    // Gets current time in milliseconds.
    public static long currentTime() {

        return System.currentTimeMillis();
    }

    // Converts milliseconds to date.
    public static String getDate(long time) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        Date date = new Date(time);
        return formatter.format(date);
    }

    // Converts milliseconds to datetime.
    public static String getDateTime(long time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
        formatter.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        Date date = new Date(time);
        return formatter.format(date);
    }

    // Converts hours/days/months and years to milliseconds.
    public static long toMilliseconds(long hours, long days, long months, long years) {

        days += months * 12 + years * 365;
        hours += 24 * days;

        return hours * 60 * 60 * 1000;
    }

    // Converts milliseconds to minutes.
    public static long minutes(long time) {
        return ((time / 1000) / 60);
    }

    // Converts milliseconds to seconds.
    public static long seconds(long time) {
        return ((time / 1000) % 60);
    }

    // Returns minute/minutes depending on the time provided.
    public static String minuteString(long time) {
        if (minutes(time) == 1) {
            return "minute";
        } else {
            return "minutes";
        }
    }

    // Returns second/seconds depending on the time provided.
    public static String secondString(long time) {
        if (seconds(time) == 1) {
            return "second";
        } else {
            return "seconds";
        }
    }
}
