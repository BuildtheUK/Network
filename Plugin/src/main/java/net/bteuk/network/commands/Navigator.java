package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.Getter;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.ChatAPI;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.commands.navigation.PreviousLocationTracker;
import net.bteuk.network.core.Constants;
import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NavigatorGui;
import net.bteuk.network.gui.navigation.ExploreGui;
import net.bteuk.network.gui.tutorials.TutorialsGui;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.sql.RegionSQL;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.staff.Moderation;
import org.btuk.minecraft.gui.GuiManager;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import teachingtutorials.utils.DBConnection;

import java.util.List;

import static net.bteuk.network.core.ServerType.TUTORIAL;

@Log
public class Navigator extends AbstractCommand {

    @Getter
    private final GuiProvider provider;
    private final Network instance;
    private final Constants constants;
    private final NavigatorGui navigator;

    public Navigator(Network instance, GuiManager guiManager, Constants constants, GlobalSQL globalSQL, RegionSQL regionSQL, RegionManager regionManager, PlotSQL plotSQL,
                     PlotAPI plotAPI, Lobby lobby, Back back, EventAPI eventAPI, ServerAPI serverAPI, Nightvision nightvision, Roles roles,
                     DBConnection tutorialsDBConnection, ChatAPI chatAPI, Moderation moderation, PreviousLocationTracker previousLocationTracker) {
        this.instance = instance;
        this.constants = constants;

        this.provider = new GuiProvider(instance, guiManager, constants, globalSQL, regionSQL, regionManager, plotSQL, plotAPI, lobby, back, eventAPI, serverAPI, nightvision, this,
                roles, tutorialsDBConnection, chatAPI, moderation, previousLocationTracker);

        this.navigator = new NavigatorGui(provider);
    }

    public void openNavigator(NetworkUser u) {
        // Check if the mainGui is not null.
        // If not then open it after refreshing its contents.
        // If no gui exists open the navigator.
        if (u.mainGui != null) {
            u.mainGui.open(u.player);
        } else {
            openMainMenu(u);
        }
    }

    public void openMainMenu(NetworkUser u) {
        u.mainGui = null;
        navigator.open(u.player);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        NetworkUser user = instance.getUser(player);

        // If u is null, cancel.
        if (user == null) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        // Check args, allows the player to open a specific menu directly.
        if (args.length > 0) {
            switch (args[0]) {
                case "explore" -> openExplore(user);
                case "building" -> openBuilding(user);
                case "tutorials" -> openTutorials(user);
                default -> openNavigator(user);
            }
        } else {
            // If the player has a previous gui, open that.
            openNavigator(user);
        }
    }

    private void openExplore(NetworkUser u) {
        if (constants.warpsEnabled()) {
            if (u.mainGui != null) {
                u.mainGui.delete();
            }
            u.mainGui = new ExploreGui(provider, u);
            u.mainGui.open(u.player);
        } else {
            openNavigator(u);
        }
    }

    private void openBuilding(NetworkUser u) {
        if (u.mainGui != null) {
            u.mainGui.delete();
        }
        u.mainGui = new BuildGui(provider, u);
        u.mainGui.open(u.player);
    }

    // Only if tutorials is enabled and the server is not already tutorials.
    private void openTutorials(NetworkUser u) {
        if (constants.serverType() != TUTORIAL && constants.tutorials()) {
            if (u.mainGui != null) {
                u.mainGui.delete();
            }
            u.mainGui = new TutorialsGui(provider, u);
            u.mainGui.open(u.player);
        } else {
            openNavigator(u);
        }
    }

    @Override
    public String getLabel() {
        return "navigator";
    }

    @Override
    public String getDescription() {
        return "Opens the main gui, will always return to the previous menu if possible.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("nav", "gui", "menu", "claim");
    }
}
