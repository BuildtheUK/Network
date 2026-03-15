package net.bteuk.network.api.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

@Getter
@Setter
@AllArgsConstructor
public class Role implements Comparable<Role> {

    private String id;

    private String name;

    private String prefix;

    private String colour;

    private int weight;

    /**
     * Get the prefix with the colour for the role.
     *
     * @return the {@link Component} prefix
     */
    public Component getColouredPrefix() {
        return Component.text(prefix, TextColor.fromHexString(colour));
    }

    public Component getColouredRoleName() {
        return Component.text(name, TextColor.fromHexString(colour));
    }

    @Override
    public int compareTo(Role o) {
        return o.getWeight() - weight;
    }
}
