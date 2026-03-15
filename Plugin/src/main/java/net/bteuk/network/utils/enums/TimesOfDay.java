package net.bteuk.network.utils.enums;

public enum TimesOfDay {

    SUNRISE("sunrise", 23000),
    DAY("day", 1000),

    MORNING("morning", 1000),

    NOON("noon", 6000),

    AFTERNOON("afternoon", 11000),

    SUNSET("sunset", 13000),

    NIGHT("night", 13000),

    MIDNIGHT("midnight", 18000);

    public final String label;
    public final int ticks;

    TimesOfDay(String label, int ticks) {
        this.label = label;
        this.ticks = ticks;
    }
}
