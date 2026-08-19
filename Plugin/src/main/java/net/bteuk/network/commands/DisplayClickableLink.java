package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;

/**
 * Basic command which can send a message with a clickable link if the link is not null.
 */
public class DisplayClickableLink extends AbstractCommand {

    private final String label;

    private final String description;

    private final String message;

    private final String link;

    private final String[] aliases;

    public DisplayClickableLink(String label, String description, String message, String link, String... aliases) {
        this.label = label;
        this.description = description;
        this.message = message;
        this.link = link;
        this.aliases = aliases;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList(aliases);
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String @NonNull [] args) {
        sendClickableLink(commandSourceStack.getSender());
    }

    protected void sendClickableLink(CommandSender sender) {
        if (link == null) {
            return;
        }

        Component discord = ChatUtils.success(message);
        discord = discord.clickEvent(ClickEvent.openUrl(link));
        sender.sendMessage(discord);
    }
}
