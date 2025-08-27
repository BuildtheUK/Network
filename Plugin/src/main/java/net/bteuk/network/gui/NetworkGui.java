package net.bteuk.network.gui;

import net.bteuk.minecraft.gui.Gui;
import net.bteuk.minecraft.gui.GuiAction;
import net.bteuk.minecraft.gui.GuiManager;
import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class NetworkGui extends Gui {

    private final Network instance;

    public NetworkGui(Network instance, GuiManager manager, int inventorySize, Component inventoryName) {
        super(manager, inventorySize, inventoryName);
        this.instance = instance;
    }

    public void setItem(int slot, ItemStack stack, NetworkGuiAction action) {
        GuiAction guiAction = clickEvent -> {
            if (!(clickEvent.getWhoClicked() instanceof Player player)) {
                return;
            }
            NetworkUser u = instance.getUser(player);

            if (u != null) {
                action.click(u);
            }
        };
        this.setItem(slot, stack, guiAction);
    }
}
