package net.bteuk.network.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public abstract class NetworkRefreshableGui extends NetworkGui implements RefreshableGui {

    public NetworkRefreshableGui(GuiProvider provider, int inventorySize, Component inventoryName) {
        super(provider, inventorySize, inventoryName);
    }

    protected abstract void createGui();

    /**
     * Ensures the gui is refreshed on opening.
     *
     * @param player the player to open the gui for
     */
    @Override
    public void open(Player player) {
        this.refresh();
        super.open(player);
    }

    /**
     * Refresh the gui by clearing it and creating it again.
     */
    @Override
    public void refresh() {
        clear();
        createGui();
    }
}
