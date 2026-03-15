package net.bteuk.network.gui;

import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

public class UtilsGui extends NetworkGui {

    public UtilsGui(GuiProvider provider) {
        super(provider, 27, Component.text("Building Utils", NamedTextColor.AQUA, TextDecoration.BOLD));
        createGui();
    }

    public void createGui() {

        // Get debug stick.
        setItem(13, Utils.createItem(Material.DEBUG_STICK, 1, Utils.title("Debug Stick"), Utils.line("Click to get the debug stick.")), (NetworkUser u) -> {

            // Delete this gui.
            u.player.closeInventory();

            // Run the command to get the item.
            u.player.performCommand("debugstick");
        });

        // Get light block.
        setItem(11, Utils.createItem(Material.LIGHT, 1, Utils.title("Light"), Utils.line("Click to get a light.")), (NetworkUser u) -> {

            // Delete this gui.
            u.player.closeInventory();

            // Run the command to get the item.
            u.player.performCommand("light");
        });

        // Get barrier block.
        setItem(15, Utils.createItem(Material.BARRIER, 1, Utils.title("Barrier"), Utils.line("Click to get a barrier.")), (NetworkUser u) -> {

            // Delete this gui.
            u.player.closeInventory();

            // Run the command to get the item.
            u.player.performCommand("barrier");
        });

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the navigator main menu.")), (NetworkUser u) -> {

            // Delete this gui.
            this.delete();

            // Switch to navigation menu.
            u.mainGui = new BuildGui(provider, u);
            u.mainGui.open(u.player);
        });
    }
}
