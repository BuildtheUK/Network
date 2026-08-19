package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.commands.AbstractCommand;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Log
public class TpToggle extends AbstractCommand {

    private final Network instance;

    private final GlobalSQL globalSQL;

    public TpToggle(Network instance) {
        this.instance = instance;
        this.globalSQL = instance.getGlobalSQL();
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

        // Invert status.
        if (globalSQL.hasRow("SELECT uuid FROM player_data WHERE uuid='" + player.getUniqueId() + "' AND teleport_enabled=1;")) {
            // Disable teleport.
            user.setTeleportEnabled(false);
            globalSQL.update("UPDATE player_data SET teleport_enabled=0 WHERE uuid='" + player.getUniqueId() + "';");

            player.sendMessage(ChatUtils.success("Other players will now no longer be able to teleport to you."));
        } else {
            // Enable teleport.
            user.setTeleportEnabled(true);
            globalSQL.update("UPDATE player_data SET teleport_enabled=1 WHERE uuid='" + player.getUniqueId() + "';");

            player.sendMessage(ChatUtils.success("Other players will be now be able to teleport to you."));
        }
    }

    @Override
    public String getLabel() {
        return "teleporttoggle";
    }

    @Override
    public String getDescription() {
        return "Enables/Disables the ability for other players to teleport to you.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("tptoggle", "toggleteleport", "toggletp");
    }
}
