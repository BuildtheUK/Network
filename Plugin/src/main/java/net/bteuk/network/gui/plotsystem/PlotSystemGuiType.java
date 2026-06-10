package net.bteuk.network.gui.plotsystem;

import org.btuk.minecraft.gui.GuiType;

/**
 * Plot system gui's that can be opened from a Network gui.
 * This allows Network to register a return function without the plot system requiring additional context.
 */
public enum PlotSystemGuiType implements GuiType {
    PLOT_MENU,
    ZONE_MENU,
    PLOT_SYSTEM_LOCATIONS,
    PLOT_SERVER_LOCATIONS
}
