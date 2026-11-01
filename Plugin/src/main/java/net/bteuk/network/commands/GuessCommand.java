package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.btuk.network.lib.utils.ChatUtils;
import org.btuk.proxy.core.user.CoreUserManager;
import org.btuk.proxy.core.user.User;
import org.btuk.proxy.core.waypoint.WaypointManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class GuessCommand extends AbstractCommand {

    private final WaypointManager waypointManager;
    private final CoreUserManager coreUserManager;

    public GuessCommand(WaypointManager waypointManager, CoreUserManager coreUserManager) {
        this.waypointManager = waypointManager;
        this.coreUserManager = coreUserManager;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NonNull [] args) {
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        User user = coreUserManager.getUserByUuid(player.getUniqueId().toString());
        if (user == null) {
            player.sendMessage(ChatUtils.error("User data not loaded. Please wait a moment."));
            return;
        }

        if (waypointManager == null || !waypointManager.isEnabled()) {
            player.sendMessage(ChatUtils.error("The Waypoint minigame is currently disabled."));
            return;
        }

        waypointManager.executeGuessCommand(user, player.hasPermission("waypoint.use"));
    }

    @Override
    public String getLabel() {
        return "guess";
    }

    @Override
    public String getDescription() {
        return "Submit your current position as a guess in the Waypoint minigame.";
    }
}
