package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.lobby.Lobby;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Rules extends AbstractCommand {

    private final Lobby lobby;

    public Rules(Lobby lobby) {
        this.lobby = lobby;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Open rules book.
        player.openBook(lobby.getRules());
    }

    @Override
    public String getLabel() {
        return "rules";
    }

    @Override
    public String getDescription() {
        return "Get rules book.";
    }
}
