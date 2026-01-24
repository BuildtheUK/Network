package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.SocketHandlerImpl;
import net.bteuk.network.commands.tabcompleters.PlayerSelector;
import net.bteuk.network.lib.dto.MuteEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * Implementation of pmute and punmute. The only difference between the 2 is the boolean that indicates whether it's
 * a mute or unmute.
 */
public abstract class PmuteAction extends AbstractCommand {

    private final Component error;

    private final Network instance;

    protected PmuteAction(Network instance, Component error) {
        this.instance = instance;
        this.error = error;
        setTabCompleter(new PlayerSelector(instance));
    }

    public void onCommand(CommandSourceStack stack, String[] args, boolean mute) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Get the uuid of the player.
        if (args.length != 1) {
            player.sendMessage(error);
            return;
        }

        // Search for the uuid of the player.
        String uuid = instance.getGlobalSQL().getString("SELECT uuid FROM player_data WHERE name='" + args[0] + "';");
        if (uuid == null) {
            player.sendMessage(error);
            return;
        }

        MuteEvent muteEvent = new MuteEvent(player.getUniqueId().toString(), uuid, mute);
        // Feedback will be sent through a direct message to the player by the proxy.
        SocketHandlerImpl.sendSocketMessageIfOnline(muteEvent);
    }
}
