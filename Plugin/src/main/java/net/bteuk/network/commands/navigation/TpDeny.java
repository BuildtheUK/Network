package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.PlayerSelector;
import org.btuk.network.lib.dto.OnlineUser;
import org.btuk.network.lib.dto.TeleportEvent;
import org.btuk.network.lib.enums.TeleportRequestType;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.socket.MessageSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public class TpDeny extends AbstractCommand {

    private final Network instance;
    private final MessageSender messageSender;

    public TpDeny(Network instance, MessageSender messageSender) {
        this.instance = instance;
        this.messageSender = messageSender;
        setTabCompleter(new PlayerSelector(instance));
    }

    @Override
    public void execute(@NonNull CommandSourceStack stack, String @NonNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Check if args exist.
        if (args.length == 0) {
            player.sendMessage(ChatUtils.error("/tpdeny <player>"));
            return;
        }

        Optional<OnlineUser> optionalRequester = instance.getOnlineUserByNameIgnoreCase(args[0]);

        if (optionalRequester.isEmpty()) {
            player.sendMessage(ChatUtils.error("Player %s is not online!", args[0]));
            return;
        }

        TeleportEvent teleportEvent = new TeleportEvent(optionalRequester.get().getUuid(), player.getUniqueId().toString(), TeleportRequestType.DENY);
        messageSender.sendSocketMessage(teleportEvent);
    }

    @Override
    public String getLabel() {
        return "tpdeny";
    }

    @Override
    public String getDescription() {
        return "Deny a teleport request.";
    }
}
