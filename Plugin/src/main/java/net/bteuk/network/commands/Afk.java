package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.core.Time;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Log
public class Afk extends AbstractCommand {

    private final Network instance;

    private final CustomChat chat;

    public Afk(Network instance, CustomChat chat) {
        this.instance = instance;
        this.chat = chat;
    }

    public void updateAfkStatus(NetworkUser user, boolean afk) {

        // Broadcast the afk message and send a user update event.
        chat.broadcastAFK(user.player, afk);

        UserUpdate userUpdateEvent = new UserUpdate();
        userUpdateEvent.setUuid(user.player.getUniqueId().toString());
        userUpdateEvent.setAfk(afk);
        chat.sendSocketMessage(userUpdateEvent);
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {

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

        // Switch afk status.
        if (user.isAfk()) {
            // Reset last logged time.
            user.last_movement = Time.currentTime();
            user.setAfk(false);
            updateAfkStatus(user, false);
        } else {
            user.setAfk(true);
            updateAfkStatus(user, true);
        }
    }

    @Override
    public String getLabel() {
        return "afk";
    }

    @Override
    public String getDescription() {
        return "Toggles afk status.";
    }
}
