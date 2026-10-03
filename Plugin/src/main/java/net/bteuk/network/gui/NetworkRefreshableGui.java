package net.bteuk.network.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class NetworkRefreshableGui extends NetworkGui implements RefreshableGui {

    private boolean loading = false;

    public NetworkRefreshableGui(GuiProvider provider, int inventorySize, Component inventoryName) {
        super(provider, inventorySize, inventoryName);
    }

    public NetworkRefreshableGui(GuiProvider provider, Inventory inventory) {
        super(provider, inventory);
    }

    protected abstract void createGui();

    /**
     * Optional method to load data asynchronously.
     * If overridden, it should call {@link #refresh()} on the main thread when finished.
     */
    protected void loadData() {
        // Default implementation does nothing and expects createGui to handle everything.
    }

    /**
     * Ensures the gui is refreshed on opening.
     *
     * @param player the player to open the gui for
     */
    @Override
    public void open(Player player) {
        if (!loading) {
            loading = true;
            provider.instance().getServer().getScheduler().runTaskAsynchronously(provider.instance(), () -> {
                loadData();
                provider.instance().getServer().getScheduler().runTask(provider.instance(), () -> {
                    loading = false;
                    this.refresh();
                });
            });
        }
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
