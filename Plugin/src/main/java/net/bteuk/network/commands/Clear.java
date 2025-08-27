package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Clear extends AbstractCommand {

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check permission.
        if (!hasPermission(player, "uknet.clear")) {
            return;
        }

        // Clear inventory.
        player.getInventory().clear();
        player.sendMessage(ChatUtils.success("Cleared your inventory."));
    }

    @Override
    public String getLabel() {
        return "clear";
    }

    @Override
    public String getDescription() {
        return "Clears your inventory.";
    }
}
