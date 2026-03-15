package net.bteuk.network.gui;

import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import org.btuk.minecraft.gui.Gui;
import org.btuk.minecraft.gui.GuiAction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class NetworkGui extends Gui {

    protected final GuiProvider provider;

    private final int inventorySize;

    public NetworkGui(GuiProvider provider, int inventorySize, Component inventoryName) {
        super(provider.manager(), inventorySize, inventoryName);
        this.provider = provider;
        this.inventorySize = inventorySize;
    }

    public NetworkGui(GuiProvider provider, Inventory inventory) {
        super(provider.manager(), inventory);
        this.provider = provider;
        this.inventorySize = inventory.getSize();
    }

    public void setItem(int slot, ItemStack stack, NetworkGuiAction action) {
        this.setItem(slot, stack, getGuiAction(action));
    }

    public void setAction(int slot, NetworkGuiAction action) {
        super.setAction(slot, getGuiAction(action));
    }

    protected void addReturnToLastSlot(NetworkGuiAction action, Component... description) {
        setItem((inventorySize - 1), Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), description), action);
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
