package net.bteuk.network.utils.plotsystem;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
public enum ReviewSelection {
    GOOD(Component.text("[Good]", NamedTextColor.GREEN),
            Component.text("[G]", NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(Component.text("Good")))),
    OK(Component.text("[Ok]", NamedTextColor.GOLD),
            Component.text("[O]", NamedTextColor.GOLD).hoverEvent(HoverEvent.showText(Component.text("Ok")))),
    POOR(Component.text("[Poor]", NamedTextColor.RED),
            Component.text("[P]", NamedTextColor.RED).hoverEvent(HoverEvent.showText(Component.text("Poor")))),
    NONE(Component.empty(), Component.empty());

    private final Component displayComponent;

    private final Component abbreviatedComponent;

    ReviewSelection(Component displayComponent, Component abbreviatedComponent) {
        this.displayComponent = displayComponent;
        this.abbreviatedComponent = abbreviatedComponent;
    }
}
