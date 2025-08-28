package net.bteuk.network.gui;

import net.bteuk.minecraft.gui.GuiManager;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.Navigator;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.sql.RegionSQL;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.Roles;
import teachingtutorials.utils.DBConnection;

/**
 * Provides gui's with all the necessary dependencies.
 */
public record GuiProvider(Network instance, GuiManager manager, Constants constants, GlobalSQL globalSQL, RegionSQL regionSQL, RegionManager regionManager, PlotSQL plotSQL,
                          PlotAPI plotAPI, Lobby lobby, Back back, EventAPI eventAPI, ServerAPI serverAPI, Nightvision nightvision, Navigator navigator, Roles roles,
                          DBConnection tutorialsDBConnection) {
}
