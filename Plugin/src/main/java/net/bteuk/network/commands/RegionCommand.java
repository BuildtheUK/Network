package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.core.Constants;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RegionCommand extends AbstractCommand {

    private final RegionManager regionManager;
    private final EventAPI eventAPI;
    private final Constants constants;

    public RegionCommand(RegionManager regionManager, EventAPI eventAPI, Constants constants) {
        this.regionManager = regionManager;
        this.eventAPI = eventAPI;
        this.constants = constants;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatUtils.error("/region join <region>"));
            return;
        }

        // Check if the first arg is 'join'
        if (!args[0].equals("join")) {
            player.sendMessage(ChatUtils.error("/region join <region>"));
            return;
        }

        // Check if the region exists.
        if (regionManager.exists(args[1])) {

            // Get the region.
            Region region = regionManager.getRegion(args[1]);

            // Check if they have an invitation for this region.
            if (regionManager.hasInvite(region, player.getUniqueId().toString())) {

                // Check if the player has permission, else notify the player accordingly.
                if (player.hasPermission("uknet.regions.join")) {
                    // Add server event to join the region.
                    eventAPI.createEvent(player.getUniqueId().toString(), regionManager.getServer(region), "region join " + region.regionName());
                } else {
                    // Send error.
                    player.sendMessage(ChatUtils.error("You do not have permission to join regions."));
                    player.sendMessage(ChatUtils.error("To join regions you need at least "+constants.minrankRegionClaim()  +"."));
                    player.sendMessage(ChatUtils.error("For more information type ").append(Component.text("/help building", NamedTextColor.DARK_RED)));
                }

                // Remove invite.
                regionManager.removeInvite(region, player.getUniqueId().toString());
            } else {
                player.sendMessage(ChatUtils.error("You have not been invited to join this region."));
            }
        } else {
            player.sendMessage(ChatUtils.error("The region ").append(Component.text(args[1], NamedTextColor.DARK_RED)).append(ChatUtils.error(" does not exist.")));
        }
    }

    @Override
    public String getLabel() {
        return "region";
    }

    @Override
    public String getDescription() {
        return "Allows players to manipulate regions without using the gui.";
    }
}
