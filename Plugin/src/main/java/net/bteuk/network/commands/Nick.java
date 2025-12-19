package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Log
public class Nick extends AbstractCommand {

    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private static final LegacyComponentSerializer COLOUR_SERIALIZER = LegacyComponentSerializer.builder().hexColors().character('&').build();

    private final Network instance;

    private final CustomChat chat;

    public Nick(Network instance, CustomChat chat) {
        this.instance = instance;
        this.chat = chat;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        if (!hasPermission(player, "network.nick")) {
            return;
        }

        NetworkUser user = instance.getUser(player);

        // If the user is null, cancel.
        if (user == null) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtils.error("Usage: %s (Supports &colours or &#RRGGBB)", "/nick <name>"));
            player.sendMessage(ChatUtils.error("Example: %s", "/nick &6My &#FF00FFName"));
            return;
        } else if ("reset".equalsIgnoreCase(args[0])) {
            Component defaultName = Component.text(player.getName());
            updateDisplayName(player, defaultName);
            player.sendMessage(ChatUtils.success("Reset display name."));
            return;

        }

        String rawName = String.join(" ", args);
        Component displayName = COLOUR_SERIALIZER.deserialize(rawName);

        // Optional: Check visible length (strip colors)
        String stripped = PLAIN_SERIALIZER.serialize(displayName);
        if (stripped.length() > 16) {
            player.sendMessage(ChatUtils.error("Your nickname must be not exceed 16 characters."));
            return;
        }

        updateDisplayName(player, displayName);
    }

    private void updateDisplayName(Player player, Component displayName) {
        UserUpdate userUpdateEvent = new UserUpdate();
        userUpdateEvent.setUuid(player.getUniqueId().toString());
        userUpdateEvent.setDisplayName(displayName);
        chat.sendSocketMessage(userUpdateEvent);
    }

    @Override
    public String getLabel() {
        return "nick";
    }

    @Override
    public String getDescription() {
        return "Updates your display name. Supports & colors and &#RRGGBB hex colors.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("nickname");
    }
}
