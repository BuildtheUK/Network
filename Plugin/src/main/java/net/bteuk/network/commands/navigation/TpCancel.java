package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.socket.MessageSender;
import org.btuk.network.lib.dto.TeleportEvent;
import org.btuk.network.lib.enums.TeleportRequestType;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class TpCancel extends AbstractCommand {

    private final MessageSender messageSender;

    public TpCancel(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void execute(@NonNull CommandSourceStack stack, String @NonNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        TeleportEvent teleportEvent = new TeleportEvent(player.getUniqueId().toString(), null, TeleportRequestType.CANCEL);
        messageSender.sendSocketMessage(teleportEvent);
    }

    @Override
    public String getLabel() {
        return "tpcancel";
    }

    @Override
    public String getDescription() {
        return "Cancel all teleport requests.";
    }
}
