package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Role;
import net.bteuk.network.utils.Roles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

@Log
public class Discord extends AbstractCommand {

    private final Network instance;
    private final CustomChat chat;
    private final Roles roles;
    private final Constants constants;

    public Discord(Network instance, CustomChat chat, Roles roles, Constants constants) {
        this.instance = instance;
        this.chat = chat;
        this.roles = roles;
        this.constants = constants;
        setTabCompleter(new FixedArgSelector(Arrays.asList("link", "unlink"), 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        if (args.length > 0) {

            NetworkUser user = instance.getUser(player);

            // If u is null, cancel.
            if (user == null) {
                log.severe("User " + player.getName() + " can not be found!");
                player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
                return;
            }

            // If discord linking is enabled
            if (args[0].equalsIgnoreCase("link")) {

                // Check if account isn't already linked, send info to unlink.
                if (user.isLinked) {
                    player.sendMessage(ChatUtils.error("You are already linked, to unlink do %s", "/discord unlink"));
                    return;
                }

                // Send random code in chat, this must be sent to the UK Bot to link your discord account.
                // Create random code from the last 6 digits of the time.
                String time = String.valueOf(Time.currentTime());
                String token = time.substring(time.length() - 6);

                DiscordLinking discordLinking = new DiscordLinking();
                discordLinking.setUuid(player.getUniqueId().toString());
                discordLinking.setToken(token);

                chat.sendSocketMessage(discordLinking);

                player.sendMessage(ChatUtils.success("To link your Discord please DM the code %s to the UK Bot within" + " the next 5 minutes.", token));
                return;
            } else if (args[0].equalsIgnoreCase("unlink")) {

                // Check if account is not linked, then ask user to link first.
                if (!user.isLinked) {
                    player.sendMessage(ChatUtils.error("You are not linked, to link do %s", "/discord link"));
                    return;
                }

                // Remove linked roles from discord, then unlink.
                Role role = roles.builderRole(user.player);

                // Remove the role in discord.
                if (role == null) {
                    user.sendMessage(ChatUtils.error("You have an invalid role, please contact an administrator."));
                    return;
                }

                DiscordRole discordRole = new DiscordRole(user.player.getUniqueId().toString(), role.getId(), false);
                chat.sendSocketMessage(discordRole);

                DiscordLinking discordLinking = new DiscordLinking();
                discordLinking.setUuid(player.getUniqueId().toString());
                discordLinking.setDiscordId(user.getDiscordId());
                discordLinking.setUnlink(true);
                chat.sendSocketMessage(discordLinking);

                user.isLinked = false;
                player.sendMessage(ChatUtils.success("Unlinked your Discord."));
                return;
            }
        }

        Component discord = ChatUtils.success("Join our discord: " + constants.discordLink());
        discord = discord.clickEvent(ClickEvent.openUrl(Objects.requireNonNull(constants.discordLink())));
        stack.getSender().sendMessage(discord);
    }

    @Override
    public String getLabel() {
        return "discord";
    }

    @Override
    public String getDescription() {
        return "Sends a link to our discord server.";
    }
}