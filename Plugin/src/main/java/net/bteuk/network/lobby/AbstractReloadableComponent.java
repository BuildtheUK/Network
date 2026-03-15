package net.bteuk.network.lobby;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractReloadableComponent implements LobbyComponent {

    private boolean enabled = false;

    /**
     * Unloads the component if enabled, and then loads it.
     */
    public final void reload() {
        if (enabled) {
            unload();
        }

        load();
    }

    /**
     * Load method.
     */
    public abstract void load();

    /**
     * Unload method.
     */
    public abstract void unload();
}
