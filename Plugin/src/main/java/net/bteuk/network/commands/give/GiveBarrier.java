package net.bteuk.network.commands.give;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Gives a barrier to the player.
 * Extends GiveItem, which handles the actual giving of the item.
 */
public class GiveBarrier extends GiveItem {
    public GiveBarrier(Network instance) {
        super(instance);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        onCommand(stack, "uknet.barrier", ItemStack.of(Material.BARRIER), "Barrier");
    }

    @Override
    public String getLabel() {
        return "barrier";
    }

    @Override
    public String getDescription() {
        return "Get a barrier block.";
    }
}
