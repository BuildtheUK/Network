package net.bteuk.network.gui;

import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Generic implementation of a multi-page gui.
 */
public abstract class NetworkMultiPageGui extends NetworkRefreshableGui {

    private final int buttonCount;

    private final int buttonsPerPage;

    private int page = 1;

    public NetworkMultiPageGui(GuiProvider provider, int size, Component title, int buttonCount) {
        super(provider, size, title);
        this.buttonCount = buttonCount;

        this.buttonsPerPage = ((size / 9) - 2) * 7;
    }

    protected abstract void createPageButton(int slot, int index);

    protected abstract void addAdditionalButtons();

    protected void createGui() {

        // Make a button for each plot.
        for (int slot = 10, index = buttonsPerPage * (page - 1);
             index < (buttonsPerPage * page) && index < buttonCount;
             index++, slot += (slot % 9 == 7) ? 3 : 1
        ) {
            createPageButton(slot, index);
        }

        // If page is greater than 1 add a previous page button.
        int row = ((getInventorySize() / 9) + 1) / 2;
        if (page > 1) {
            setItem((row - 1) * 9, Utils.createItem(Material.ARROW, 1, Utils.title("Previous Page"), Utils.line("Return to the previous page.")), (NetworkUser u) -> {
                page--;
                this.refresh();
                this.updatePlayerInventory(u.player);
            });
        }

        // If more items exist than fit on the page, show the next page button.
        if ((buttonsPerPage * page) < buttonCount) {
            setItem((row * 9) - 1, Utils.createItem(Material.ARROW, 1, Utils.title("Next Page"), Utils.line("Go to the next page.")), (NetworkUser u) -> {
                page++;
                this.refresh();
                updatePlayerInventory(u.player);
            });
        }

        addAdditionalButtons();
    }
}
