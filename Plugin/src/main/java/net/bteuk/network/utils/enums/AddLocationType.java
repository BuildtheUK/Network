package net.bteuk.network.utils.enums;

public enum AddLocationType {
    ADD("Add"),
    REVIEW("Review"),
    UPDATE("Update");

    public final String label;

    AddLocationType(String label) {
        this.label = label;
    }
}
