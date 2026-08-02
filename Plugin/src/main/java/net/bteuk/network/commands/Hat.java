package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Log
public final class Hat extends AbstractCommand {

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        if (!hasPermission(player, "uknet.hat")) {
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack helmet = player.getInventory().getHelmet();

        player.getInventory().setHelmet(heldItem);
        player.getInventory().setItemInMainHand(helmet);
        player.sendMessage(ChatUtils.success("Switched your held item with your helmet!"));
    }

    @Override
    public String getLabel() {
        return "hat";
    }

    @Override
    public String getDescription() {
        return "Switches the player's held item with the item in their helmet slot";
    }
}