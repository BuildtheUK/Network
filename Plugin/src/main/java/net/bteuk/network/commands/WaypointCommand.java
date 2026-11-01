package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.btuk.network.lib.utils.ChatUtils;
import org.btuk.proxy.core.user.CoreUserManager;
import org.btuk.proxy.core.user.User;
import org.btuk.proxy.core.waypoint.WaypointManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class WaypointCommand extends AbstractCommand {

    private final WaypointManager waypointManager;
    private final CoreUserManager coreUserManager;

    public WaypointCommand(WaypointManager waypointManager, CoreUserManager coreUserManager) {
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

        waypointManager.executeWaypointCommand(user, player.hasPermission("waypoint.use"), player.hasPermission("waypoint.admin"), args);
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
        if (waypointManager == null || !waypointManager.isEnabled()) {
            return Collections.emptyList();
        }

        CommandSender sender = commandSourceStack.getSender();
        boolean hasAdmin = sender.hasPermission("waypoint.admin");
        return waypointManager.suggestWaypointCommand(hasAdmin, args);
    }

    @Override
    public String getLabel() {
        return "waypoint";
    }

    @Override
    public String getDescription() {
        return "Waypoint daily navigation minigame.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("wp");
    }
}
