package net.bteuk.network.gui.progressmap;

import me.bteuk.progressmapper.guis.LocalFeaturesMenu;
import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkGui;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class LocalFeatureListGUI extends NetworkGui {
    // Contains all of the relevant information for each feature in the list
    private final LocalFeaturesMenu features;

    public LocalFeatureListGUI(GuiProvider provider, LocalFeaturesMenu features) {
        // Need a list of things created really. I think this has to be before this one is created. This then holds
        // all of the Features
        // Each feature would have a feature menu (not a feature page)
        super(provider, features.getGUI());
        this.features = features;
        setActions();
    }

    private void setActions() {
        int i, iFeatures;
        iFeatures = features.getNumFeatures();

        // Creates all the actions
        for (i = 0; i < iFeatures; i++) {
            final int iFinalSlot = i;
            setAction(i, (NetworkUser u) -> {
                // When a feature is clicked on it needs to open a FeaturePageGUI
                u.mainGui = new FeaturePageGUI(provider, features.getFeatureMenu(iFinalSlot), this);
                u.mainGui.open(u.player);
            });
        }

        // Back button
        setAction(getInventory().getSize() - 1, (NetworkUser u) -> {
            // Delete this gui.
            this.delete();
            u.mainGui = null;

            // Switch to plot info.
            u.mainGui = new BuildGui(u);
            u.mainGui.open(u.player);
        });
    }

    @Override
    public void open(Player player) {
        this.refresh();
        super.open(player);
    }

    public void refresh() {
        // Reloads the features (with a blank one at the end)
        features.loadFeatures(provider.constants().mapHubAPIKey());

        // Refresh icons
        this.clear();
        Inventory inventory = features.getGUI();
        this.setItemsFromInventory(inventory);

        // Refresh actions
        setActions();
    }
}
