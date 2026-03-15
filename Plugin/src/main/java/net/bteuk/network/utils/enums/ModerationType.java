package net.bteuk.network.utils.enums;

public enum ModerationType {
    BAN("Ban"),
    MUTE("Mute"),
    UNBAN("Unban"),
    UNMUTE("Unmute"),
    KICK("Kick");

    public final String label;

    ModerationType(String label) {
        this.label = label;
    }
}
