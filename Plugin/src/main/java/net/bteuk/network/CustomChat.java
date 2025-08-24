package net.bteuk.network;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.bteuk.network.api.ChatAPI;
import net.bteuk.network.eventing.listeners.Connect;
import net.bteuk.network.exceptions.NotMutedException;
import net.bteuk.network.lib.dto.AbstractTransferObject;
import net.bteuk.network.lib.dto.AddTeamEvent;
import net.bteuk.network.lib.dto.ChatMessage;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.DiscordLinking;
import net.bteuk.network.lib.dto.DiscordRole;
import net.bteuk.network.lib.dto.OnlineUserAdd;
import net.bteuk.network.lib.dto.OnlineUserRemove;
import net.bteuk.network.lib.dto.OnlineUsersReply;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserRemove;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.socket.InputSocket;
import net.bteuk.network.lib.socket.OutputSocket;
import net.bteuk.network.lib.socket.SocketHandler;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Role;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.Time;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import static net.bteuk.network.commands.Afk.updateAfkStatus;
import static net.bteuk.network.lib.enums.ChatChannels.STAFF;
import static net.bteuk.network.utils.Constants.LOGGER;
import static net.bteuk.network.utils.NetworkConfig.CONFIG;
import static net.bteuk.network.utils.staff.Moderation.getMutedComponent;
import static net.bteuk.network.utils.staff.Moderation.isMuted;

public class CustomChat implements Listener, SocketHandler, ChatAPI {

    private static final String AFK = "%s is now afk";
    private static final String NOT_AFK = "%s is no longer afk";
    private final Network instance;
    private final OutputSocket outputSocket;

    public CustomChat(Network instance) {

        this.instance = instance;

        instance.getServer().getPluginManager().registerEvents(this, instance);

        // Set up the output socket.
        outputSocket = new OutputSocket(
                CONFIG.getString("chat.socket.output.IP"),
                CONFIG.getInt("chat.socket.output.port")
        );

        // Register input socket for receiving messages from the proxy.
        int inputSocketPort = CONFIG.getInt("chat.socket.input.port");
        if (inputSocketPort == 0) {
            LOGGER.severe("Input socket port is not set in config or is set to 0. Please set a valid port!");
        } else {
            // Create the input socket.
            InputSocket inputSocket = new InputSocket(inputSocketPort);
            inputSocket.start(this);
        }

        LOGGER.info("Successfully enabled Chat!");
    }

    public static ChatMessage getChatMessage(Component component, NetworkUser u) {

        ChatMessage chatMessage = new ChatMessage();
        Component message = playerMessageFormat(u, component);

        if (u.getChatChannel().equals(STAFF.getChannelName())) {
            message = Component.text("[Staff]", NamedTextColor.RED).append(message);
            // Prefix the chat message with [Staff]
        }

        chatMessage.setChannel(u.getChatChannel());
        chatMessage.setSender(u.player.getUniqueId().toString());
        chatMessage.setComponent(message);
        return chatMessage;
    }

    public static DirectMessage getDirectMessage(String message, String senderName, String senderUuid,
                                                 String recipientName, String recipientUuid, ChatChannels channel) {
        return new DirectMessage(channel.getChannelName(), recipientUuid, senderUuid, directMessageFormat(message,
                senderName, recipientName), false);
    }

    /**
     * Format a player message to add the player prefix and name.
     *
     * @param message the {@link Component} to format
     * @return the {@link Component} formatted message
     */
    private static Component playerMessageFormat(NetworkUser user, Component message) {
        Role userRole = user.getPrimaryRole();
        return userRole.getColouredPrefix() // The prefix based on the role.
                .append(Component.space())
                .append(ChatUtils.line(user.player.getName())) // Player name in white without formatting.
                .append(Component.space())
                .append(Component.text(">", NamedTextColor.GRAY).decorate(TextDecoration.BOLD)) // Arrow between the
                // player and message in bold.
                .append(Component.space())
                .append(message.color(NamedTextColor.WHITE)); // The message in white without formatting.
    }

    private static Component directMessageFormat(String message, String sender, String recipient) {
        return ChatUtils.line("[").decorate(TextDecoration.BOLD)
                .append(ChatUtils.line(sender))
                .append(Component.space())
                .append(ChatUtils.line("->"))
                .append(Component.space())
                .append(ChatUtils.line(recipient))
                .append(ChatUtils.line("]").decorate(TextDecoration.BOLD))
                .append(Component.space())
                .append(Component.text(">", NamedTextColor.GRAY).decorate(TextDecoration.BOLD)) // Arrow between the
                // player and message in bold.
                .append(Component.space())
                .append(ChatUtils.line(message)); // The message in white without formatting.
    }

    public void onDisable() {
        instance.getServer().getMessenger().unregisterIncomingPluginChannel(instance, "uknet:network");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChatEvent(AsyncChatEvent e) {

        // If player is muted cancel.
        if (isMuted(e.getPlayer().getUniqueId().toString())) {
            e.setCancelled(true);
            try {

                // Send message and end event.
                e.getPlayer().sendMessage(getMutedComponent(e.getPlayer().getUniqueId().toString()));
                return;
            } catch (NotMutedException ex) {

                // Unset the muted status.
                e.setCancelled(false);
            }
        }

        if (!e.isCancelled()) {
            e.setCancelled(true);
            // Get user, if staff chat enabled send message to staff chat.
            NetworkUser user = instance.getUser(e.getPlayer());

            // If u is null, cancel.
            if (user == null) {
                LOGGER.severe("User " + e.getPlayer().getName() + " can not be found!");
                e.getPlayer().sendMessage(ChatUtils.error("User can not be found, please relog!"));
                return;
            }

            // Reset last movement of player, if they're afk unset that.
            user.last_movement = Time.currentTime();

            if (user.isAfk()) {
                user.setAfk(false);
                updateAfkStatus(user, false);
            }
            ChatMessage chatMessage = getChatMessage(e.message(), user);
            sendSocketMessage(chatMessage);
        }
    }

    public void sendSocketMessage(AbstractTransferObject chatMessage) {
        outputSocket.sendSocketMessage(chatMessage);
    }

    @Override
    public AbstractTransferObject handle(AbstractTransferObject abstractTransferObject) {
        if (abstractTransferObject instanceof DirectMessage directMessage) {
            handleDirectMessage(directMessage);
        } else if (abstractTransferObject instanceof DiscordLinking discordLinking) {
            handleDiscordLinking(discordLinking);
        } else if (abstractTransferObject instanceof AddTeamEvent addTeamEvent) {
            instance.getTab().handle(addTeamEvent);
        } else if (abstractTransferObject instanceof UserConnectReply userConnectReply) {
            Connect.handleUserConnectReply(userConnectReply);
        } else if (abstractTransferObject instanceof UserRemove userRemove) {
            Connect.handleUserRemove(userRemove);
        } else if (abstractTransferObject instanceof UserUpdate userUpdate) {
            handleUserUpdate(userUpdate);
        } else if (abstractTransferObject instanceof OnlineUsersReply onlineUsersReply) {
            instance.handleOnlineUsersReply(onlineUsersReply);
        } else if (abstractTransferObject instanceof OnlineUserAdd onlineUserAdd) {
            instance.handleOnlineUserAdd(onlineUserAdd);
        } else if (abstractTransferObject instanceof OnlineUserRemove onlineUserRemove) {
            instance.handleOnlineUserRemove(onlineUserRemove);
        } else {
            LOGGER.warning(String.format("Socket object has an unrecognised type %s",
                    abstractTransferObject.getClass().getTypeName()));
        }
        return null;
    }

    private void handleDirectMessage(DirectMessage message) {
        // Send the message if the player is on this server.
        instance.getServer().getOnlinePlayers().stream()
                .filter(player -> player.getUniqueId().toString().equals(message.getRecipient()))
                .forEach(player -> {
                    switch (message.getChannel()) {

                        case "global" -> player.sendMessage(message.getComponent());

                        case "staff" -> {
                            if (player.hasPermission("uknet.staff")) {
                                player.sendMessage(message.getComponent());
                            }
                        }

                        case "reviewer" -> {
                            // Send only to reviewers.
                            if (player.hasPermission("group.reviewer")) {
                                player.sendMessage(message.getComponent());
                            }
                        }
                    }
                });
    }

    private void handleDiscordLinking(DiscordLinking discordLinking) {

        if (discordLinking.isUnlink() && discordLinking.getDiscordId() != -1) {
            // Unlink, this is only used if the user is no longer in the discord server.
            // Hence why no roles need to be removed.
            for (NetworkUser user : instance.getUsers()) {
                if (user.isLinked && user.getDiscordId() == discordLinking.getDiscordId()) {
                    // Unlink
                    user.isLinked = false;
                }
            }
            return;
        }

        if (discordLinking.getUuid() == null || discordLinking.isUnlink() || discordLinking.getDiscordId() == -1) {
            return;
        }

        // Find the user.
        instance.getUsers().stream()
                .filter(user -> user.player.getUniqueId().toString().equals(discordLinking.getUuid()))
                .forEach(user -> {

                    // Link account.
                    instance.getGlobalSQL()
                            .update("INSERT INTO discord(uuid,discord_id) VALUES('" + discordLinking.getUuid() + "'," + discordLinking.getDiscordId() + ");");

                    user.isLinked = true;
                    user.setDiscordId(discordLinking.getDiscordId());

                    // Get the highest role for syncing and sync it, except for guest.
                    Role role = Roles.builderRole(user.player);

                    // Add the role in discord.
                    if (role == null) {
                        user.sendMessage(ChatUtils.error("You have an invalid role, please contact an administrator."));
                        return;
                    }

                    DiscordRole discordRole = new DiscordRole(user.player.getUniqueId().toString(), role.getId(), true);
                    outputSocket.sendSocketMessage(discordRole);

                    user.sendMessage(ChatUtils.success("Your discord has been linked!"));
                });
    }

    private void handleUserUpdate(UserUpdate userUpdate) {
        // If the user is online check if anything needs updating.
        instance.getUsers().stream().filter(user -> user.player.getUniqueId().toString().equals(userUpdate.getUuid()))
                .findFirst().ifPresent(user -> {
                    if (userUpdate.getTabPlayer() != null && !userUpdate.getTabPlayer().getPrimaryGroup()
                            .equals(user.getPrimaryRole().getId())) {
                        // Update the primary role.
                        Role primaryRole = Roles.getRoleById(userUpdate.getTabPlayer().getPrimaryGroup());
                        if (primaryRole != null) {
                            LOGGER.info(String.format("Updated primary role for %s to %s", user.player.getName(),
                                    primaryRole.getName()));
                            user.setPrimaryRole(primaryRole);
                        }
                    }
                });
    }

    // Send afk or no longer afk message to players ingame and discord.
    public void broadcastAFK(Player p, boolean afk) {

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(p.getUniqueId().toString());
        chatMessage.setChannel(ChatChannels.GLOBAL.getChannelName());

        if (afk) {
            chatMessage.setComponent(Component.text(String.format(AFK, p.getName()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true));
        } else {
            chatMessage.setComponent(Component.text(String.format(NOT_AFK, p.getName()), NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, true));
        }

        // Send message
        sendSocketMessage(chatMessage);
    }
}
