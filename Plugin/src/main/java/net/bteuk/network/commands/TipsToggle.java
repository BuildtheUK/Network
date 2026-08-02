package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Log
public class TipsToggle extends AbstractCommand {

    private final Network instance;

    public TipsToggle(Network instance) {
        this.instance = instance;
    }

    public void toggleTips(Player p) {

        // Get the NetworkUser for this player.
        NetworkUser user = instance.getUser(p);

        if (user == null) {
            log.warning("NetworkUser for player " + p.getName() + " is null!");
            return;
        }

        // If tips is enabled, disable it, else enable.
        if (user.isTipsEnabled()) {
            // Disable tips.
            user.setTipsEnabled(false);
            p.sendMessage(ChatUtils.success("Disabled tips in chat."));
        } else {
            // Enable tips.
            user.setTipsEnabled(true);
            p.sendMessage(ChatUtils.success("Enabled tips in chat."));
        }
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        toggleTips(player);
    }

    @Override
    public String getLabel() {
        return "tips";
    }

    @Override
    public String getDescription() {
        return "Toggles tips in chat.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("tipstoggle", "toggletips");
    }
}