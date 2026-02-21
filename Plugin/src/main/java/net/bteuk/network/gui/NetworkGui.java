package net.bteuk.network.gui;

import net.bteuk.minecraft.gui.Gui;
import net.bteuk.minecraft.gui.GuiAction;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class NetworkGui extends Gui {

    protected final GuiProvider provider;

    public NetworkGui(GuiProvider provider, int inventorySize, Component inventoryName) {
        super(provider.manager(), inventorySize, inventoryName);
        this.provider = provider;
    }

    public NetworkGui(GuiProvider provider, Inventory inventory) {
        super(provider.manager(), inventory);
        this.provider = provider;
    }

    public void setItem(int slot, ItemStack stack, NetworkGuiAction action) {
        this.setItem(slot, stack, getGuiAction(action));
    }

    public void setAction(int slot, NetworkGuiAction action) {
        super.setAction(slot, getGuiAction(action));
    }

    private GuiAction getGuiAction(NetworkGuiAction action) {
        return clickEvent -> {
            if (!(clickEvent.getWhoClicked() instanceof Player player)) {
                return;
            }
            NetworkUser u = provider.instance().getUser(player);

            if (u != null && action != null) {
                action.click(u);
            }
        };
    }
}
