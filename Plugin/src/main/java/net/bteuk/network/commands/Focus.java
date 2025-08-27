package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Command to enable/disable focus mode.
 */
@Log
public class Focus extends AbstractCommand {

    private final Network instance;

    public Focus(Network instance, Constants constants) {
        this.instance = instance;
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

        user.toggleFocus();
    }

    @Override
    public String getLabel() {
        return "focus";
    }

    @Override
    public String getDescription() {
        return "Toggle focus mode, hides chat and players.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("focusmode", "fm");
    }
}
