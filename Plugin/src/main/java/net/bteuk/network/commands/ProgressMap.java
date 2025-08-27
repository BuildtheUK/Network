package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.core.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ProgressMap extends AbstractCommand {

    private final Constants constants;

    public ProgressMap(Constants constants) {
        this.constants = constants;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Send them a link
        if (constants.progressMap()) {
            TextComponent textComponent = Component.text("Click here to view a map of our progress!",
                    NamedTextColor.AQUA);
            textComponent = textComponent.clickEvent(ClickEvent.openUrl(constants.progressMapLink()));
            player.sendMessage(textComponent);
        }
    }

    @Override
    public String getLabel() {
        return "progressmap";
    }

    @Override
    public String getDescription() {
        return "Sends a link of the progress map";
    }

    @Override
    public List<String> getAliases() {
        return List.of("progress");
    }
}
