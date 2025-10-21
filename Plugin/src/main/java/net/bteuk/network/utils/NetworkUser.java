package net.bteuk.network.utils;

import lombok.Getter;
import lombok.Setter;
import net.bteuk.minecraft.gui.Gui;
import net.bteuk.network.Network;
import net.bteuk.network.building_companion.BuildingCompanion;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.Time;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.lib.dto.FocusEvent;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserDisconnect;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.RegionUser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

import static net.bteuk.network.lib.enums.ChatChannels.GLOBAL;
import static net.bteuk.network.lib.enums.ChatChannels.REVIEWER;
import static net.bteuk.network.lib.enums.ChatChannels.STAFF;

public class NetworkUser {

    // Player instance.
    public final Player player;
    // Network instance.
    private final Network instance;

    private final Constants constants;
    // Main gui, includes everything that is part of the navigator.
    public Gui mainGui;

    // Lights out, a gui game.
    public LightsOut lightsOut;

    // Staff gui.
    public Gui staffGui;

    // If the player is switching server.
    @Getter
    @Setter
    private boolean switching;
    // If the player is afk.
    @Setter
    @Getter
    private boolean afk;
    public long last_movement;
    // If linked to discord.
    public boolean isLinked;
    // If the player is currently in a portal,
    // This is to prevent continuous execution of portal events.
    public boolean inPortal;
    public boolean wasInPortal;
    // The current active chat channel.
    // The default is global.
    @Getter
    @Setter
    private String chatChannel;
    // Navigator in hotbar.
    @Getter
    @Setter
    private boolean navigatorEnabled;
    @Getter
    @Setter
    private boolean teleportEnabled;
    @Getter
    @Setter
    private boolean nightvisionEnabled;
    @Getter
    @Setter
    private long discordId;
    // Should tips be displayed for the player.
    @Getter
    @Setter
    private boolean tipsEnabled;

    // Building companion tool.
    @Getter
    @Setter
    private BuildingCompanion companion;

    // If the player has the map teleport item.
    @Getter
    @Setter
    private boolean hasMapItem;

    @Getter
    @Setter
    private Role primaryRole;

    @Getter
    private boolean focusEnabled;

    private final RegionUser regionUser;

    public NetworkUser(Player player, UserConnectReply reply, Network instance, Constants constants, Roles roles, Nightvision nightvision, EventManager eventManager,
                       RegionUser regionUser) {

        this.instance = instance;
        this.constants = constants;

        this.player = player;
        this.regionUser = regionUser;

        navigatorEnabled = reply.isNavigatorEnabled();
        teleportEnabled = reply.isTeleportEnabled();
        nightvisionEnabled = reply.isNightvisionEnabled();
        chatChannel = reply.getChatChannel();
        tipsEnabled = reply.isTipsEnabled();
        setFocusEnabled(reply.isFocusEnabled());

        switching = false;
        inPortal = false;
        wasInPortal = false;
        setAfk(false);
        last_movement = Time.currentTime();

        primaryRole = roles.getPrimaryRole(player);

        // Get discord linked status.
        // If they're linked get discord id.
        isLinked = instance.getGlobalSQL().hasRow("SELECT uuid FROM discord WHERE uuid='" + player.getUniqueId() +
                "';");
        if (isLinked) {
            discordId =
                    instance.getGlobalSQL()
                            .getLong("SELECT discord_id FROM discord WHERE uuid='" + player.getUniqueId() + "';");
        }

        // If navigator is disabled, remove the navigator if in the inventory.
        if (!navigatorEnabled) {

            ItemStack slot8 = player.getInventory().getItem(8);

            if (slot8 != null) {
                if (slot8.equals(instance.getNavigatorItem())) {
                    player.getInventory().setItem(8, null);
                }
            }
        }

        runEvents(eventManager);

        // Give the player nightvision if enabled or remove it if disabled.
        if (nightvisionEnabled) {

            nightvision.giveNightvision(player);
        } else {

            nightvision.removeNightvision(player);
        }

        // If focus mode is enabled hide other players.
        if (focusEnabled) {
            hidePlayers();
        }
    }

    /**
     * Get the chat channels to which this user has access.
     *
     * @param player the players to get the chat channels for
     * @return {@link Set} set of {@link String} channels
     */
    public static Set<String> getChannels(Player player) {

        Set<String> channels = new HashSet<>();
        channels.add(GLOBAL.getChannelName());

        if (player.hasPermission("uknet.staff")) {
            channels.add(STAFF.getChannelName());
        }

        if (player.hasPermission("group.reviewer")) {
            channels.add(REVIEWER.getChannelName());
        }

        return channels;
    }

    public UserDisconnect createDisconnectEvent() {
        return new UserDisconnect(
                player.getUniqueId().toString(),
                constants.serverName(),
                isNavigatorEnabled(),
                isTeleportEnabled(),
                isNightvisionEnabled(),
                getChatChannel(),
                isTipsEnabled()
        );
    }

    private void runEvents(EventManager eventManager) {

        // Check if the player has any join events, if try run them.
        // Delay by 1 second for all plugins to run their join events.
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
            if (instance.getGlobalSQL().hasRow("SELECT uuid FROM join_events WHERE uuid='" + player.getUniqueId() + ";")) {

                // Get the event from the database.
                String event = instance.getGlobalSQL().getString("SELECT event FROM join_events WHERE uuid='" + player.getUniqueId() + "';");

                // Get message.
                String message = instance.getGlobalSQL().getString("SELECT message FROM join_events WHERE uuid='" + player.getUniqueId() + "';");

                // Split the event by word.
                String[] aEvent = event.split(" ");

                // Clear the events.
                instance.getGlobalSQL().update("DELETE FROM join_events WHERE uuid='" + player.getUniqueId() + "';");

                // Send the event to the event handler.
                eventManager.event(player.getUniqueId().toString(), aEvent, message);
            }
        }, 20L);
    }

    /**
     * Check if the {@link NetworkUser} has the permission node.
     *
     * @param permission_node permission node.
     * @return whether the {@link NetworkUser} has the permission node.
     */
    public boolean hasPermission(String permission_node) {
        return player.hasPermission(permission_node);
    }

    /**
     * Check if the {@link NetworkUser} has any permission node in the array.
     *
     * @param permission_nodes array of permission nodes.
     * @return whether the {@link NetworkUser} has any of the permission nodes.
     */
    public boolean hasAnyPermission(String... permission_nodes) {

        for (String permission_node : permission_nodes) {
            if (hasPermission(permission_node)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Sends the given message to player.
     *
     * @param message the message to send
     */
    public void sendMessage(Component message) {
        player.sendMessage(message);
    }

    public Location getLocationWithCoordinateTransform() {
        return new Location(
                player.getWorld(),
                player.getLocation().getX() + (constants.regionsEnabled() ? regionUser.getDeltaX() : 0),
                player.getLocation().getY(),
                player.getLocation().getZ() + (constants.regionsEnabled() ? regionUser.getDeltaZ() : 0),
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );
    }

    public String getUuid() {
        return player.getUniqueId().toString();
    }

    public void toggleFocus() {
        setFocusEnabled(!isFocusEnabled());
        if (isFocusEnabled()) {
            player.sendMessage(ChatUtils.success("Enabled focus mode"));
        } else {
            player.sendMessage(ChatUtils.success("Disabled focus mode"));
        }
    }

    private void setFocusEnabled(boolean enabled) {
        focusEnabled = enabled;
        if (focusEnabled) {
            hidePlayers();
        } else {
            showPlayers();
        }
        FocusEvent focusEvent = new FocusEvent(player.getUniqueId().toString(), focusEnabled);
        instance.getChat().sendSocketMessage(focusEvent);
    }

    public void hidePlayer(Player playerToHide) {
        player.hidePlayer(instance, playerToHide);
    }

    private void hidePlayers() {
        instance.getServer().getOnlinePlayers().forEach(serverPlayer -> {
            if (player != serverPlayer) {
                player.hidePlayer(instance, serverPlayer);
            }
        });
    }

    private void showPlayers() {
        instance.getServer().getOnlinePlayers().forEach(serverPlayer -> {
            if (player != serverPlayer) {
                player.showPlayer(instance, serverPlayer);
            }
        });
    }

}
