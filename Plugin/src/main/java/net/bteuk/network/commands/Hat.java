package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@Log
public class Hat extends AbstractCommand {

    private final Network instance;

    public Hat(Network instance) {
        this.instance = instance;
    }

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

        NetworkUser user = instance.getUser(player);

        // If the user is null, cancel.
        if (user == null) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
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
