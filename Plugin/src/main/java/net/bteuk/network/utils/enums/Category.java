package net.bteuk.network.utils.enums;

import lombok.Getter;

@Getter
public enum Category {

    EXPLORE("Explore", false),

    ENGLAND("England", true),
    SCOTLAND("Scotland", true),
    WALES("Wales", true),
    NORTHERN_IRELAND("Northern Ireland", true),
    OTHER("Other", true),

    NEARBY("Nearby", false),
    SEARCH("Search", false),
    SUGGESTED("Suggested", false),

    SUBCATEGORY("Subcategory", false),
    TEMPORARY("Temporary", false);

    private final String label;

    private final boolean selectable;

    Category(String label, boolean selectable) {
        this.label = label;
        this.selectable = selectable;
    }
}
