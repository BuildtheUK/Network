package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.core.Constants;
import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.NavigatorGui;
import net.bteuk.network.gui.navigation.ExploreGui;
import net.bteuk.network.gui.tutorials.TutorialsGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.bteuk.network.core.ServerType.TUTORIAL;

@Log
public class Navigator extends AbstractCommand {

    private final Network instance;
    private final Constants constants;
    private final NavigatorGui navigator;

    public Navigator(Network instance, Constants constants, Lobby lobby, Back back, EventAPI eventAPI, ServerAPI serverAPI) {
        this.instance = instance;
        this.constants = constants;
        navigator = new NavigatorGui(constants, instance.getGlobalSQL(), lobby, back, eventAPI, serverAPI);
    }

    public void openNavigator(NetworkUser u) {
        // Check if the mainGui is not null.
        // If not then open it after refreshing its contents.
        // If no gui exists open the navigator.
        if (u.mainGui != null) {
            u.mainGui.refresh();
            u.mainGui.open(u);
        } else {
            navigator.open(u);
        }
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
            u.mainGui = new ExploreGui(u);
            u.mainGui.open(u);
        } else {
            openNavigator(u);
        }
    }

    private void openBuilding(NetworkUser u) {
        if (u.mainGui != null) {
            u.mainGui.delete();
        }
        u.mainGui = new BuildGui(u);
        u.mainGui.open(u);
    }

    // Only if tutorials is enabled and the server is not already tutorials.
    private void openTutorials(NetworkUser u) {
        if (constants.serverType() != TUTORIAL && constants.tutorials()) {
            if (u.mainGui != null) {
                u.mainGui.delete();
            }
            u.mainGui = new TutorialsGui(u);
            u.mainGui.open(u);
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
