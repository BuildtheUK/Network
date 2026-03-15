package net.bteuk.network.utils.plotsystem;

import lombok.Getter;

@Getter
public enum ReviewCategory {

    GENERAL("General", false),

    OUTLINES("Outlines", true),
    FEATURES("Features", true),
    ROOF("Roof", true),
    GARDEN("Garden", true),
    TEXTURES("Textures", true),
    DETAILS("Details", true);

    private final String displayName;

    private final boolean required;

    ReviewCategory(String displayName, boolean required) {
        this.displayName = displayName;
        this.required = required;
    }
}
