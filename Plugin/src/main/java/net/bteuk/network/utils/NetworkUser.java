package net.bteuk.network.utils;

import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.Network;
import net.bteuk.network.building_companion.BuildingCompanion;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.gui.Gui;
import net.bteuk.network.lib.dto.DirectMessage;
import net.bteuk.network.lib.dto.FocusEvent;
import net.bteuk.network.lib.dto.UserConnectReply;
import net.bteuk.network.lib.dto.UserDisconnect;
import net.bteuk.network.lib.enums.ChatChannels;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.regions.Region;
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
import static net.bteuk.network.utils.Constants.EARTH_WORLD;
import static net.bteuk.network.utils.Constants.REGIONS_ENABLED;
import static net.bteuk.network.utils.Constants.SERVER_NAME;
import static net.bteuk.network.utils.Constants.SERVER_TYPE;
import static net.bteuk.network.utils.enums.ServerType.EARTH;
import static net.bteuk.network.utils.enums.ServerType.PLOT;

public class NetworkUser {

    // Player instance.
    public final Player player;
    // Network instance.
    private final Network instance;
    // Main gui, includes everything that is part of the navigator.
    public Gui mainGui;

    // Lights out, a gui game.
    public LightsOut lightsOut;

    // Staff gui.
    public Gui staffGui;
    // Region information.
    public boolean inRegion;
    public Region region;
    public int dx;
    public int dz;
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

    public NetworkUser(Player player, UserConnectReply reply) {

        this.instance = Network.getInstance();

        this.player = player;

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

        primaryRole = Roles.getPrimaryRole(player);

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
                if (slot8.equals(instance.navigator)) {
                    player.getInventory().setItem(8, null);
                }
            }
        }

        // Check if the player is in a region.
        if (REGIONS_ENABLED) {
            if (SERVER_TYPE == EARTH) {
                // Check if they are in the earth world.
                if (player.getWorld().getName().equals(EARTH_WORLD)) {
                    region = instance.getRegionManager().getRegion(player.getLocation());
                    // Add region to database if not exists.
                    region.addToDatabase();
                    inRegion = true;
                }
            } else if (SERVER_TYPE == PLOT) {
                // Check if the player is in a buildable plot world and apply coordinate transform if true.
                if (instance.getPlotSQL()
                        .hasRow("SELECT name FROM location_data WHERE name='" + player.getLocation().getWorld()
                                .getName() + "';")) {
                    updateCoordinateTransform(instance.getPlotSQL(), player.getLocation());

                    region = instance.getRegionManager().getRegion(player.getLocation(), dx, dz);
                    inRegion = true;
                }
            }
        }

        runEvents();

        // Give the player nightvision if enabled or remove it if disabled.
        if (nightvisionEnabled) {

            Nightvision.giveNightvision(player);
        } else {

            Nightvision.removeNightvision(player);
        }

        // If focus mode is enabled hide other players.
        if (focusEnabled) {
            hidePlayers();
        }
    }

    /**
     * Get the name of a user.
     *
     * @param uuid uuid of the user
     * @return {@link String} name of the user
     */
    public static String getName(String uuid) {
        return Network.getInstance().getGlobalSQL().getString("SELECT name FROM player_data WHERE uuid='" + uuid +
                "';");
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

    /**
     * Send the user an offline message. This also works for online players.
     *
     * @param uuid    uuid of the user
     * @param message the message to send
     */
    public static void sendOfflineMessage(String uuid, Component message) {
        DirectMessage directMessage = new DirectMessage(ChatChannels.GLOBAL.getChannelName(), uuid, "server", message
                , true);
        Network.getInstance().getChat().sendSocketMessage(directMessage);
    }

    public UserDisconnect createDisconnectEvent() {
        return new UserDisconnect(
                player.getUniqueId().toString(),
                SERVER_NAME,
                isNavigatorEnabled(),
                isTeleportEnabled(),
                isNightvisionEnabled(),
                getChatChannel(),
                isTipsEnabled()
        );
    }

    private void runEvents() {

        // Check if the player has any join events, if try run them.
        // Delay by 1 second for all plugins to run their join events.
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
            if (instance.getGlobalSQL().hasRow("SELECT uuid FROM join_events WHERE uuid='" + player.getUniqueId() +
                    "' AND type='network';")) {

                // Get the event from the database.
                String event =
                        instance.getGlobalSQL().getString(
                                "SELECT event FROM join_events WHERE uuid='" + player.getUniqueId() + "' AND " +
                                        "type='network'");

                // Get message.
                String message =
                        instance.getGlobalSQL().getString(
                                "SELECT message FROM join_events WHERE uuid='" + player.getUniqueId() + "' AND " +
                                        "type='network'");

                // Split the event by word.
                String[] aEvent = event.split(" ");

                // Clear the events.
                instance.getGlobalSQL().update("DELETE FROM join_events WHERE uuid='" + player.getUniqueId() + "' AND" +
                        " type='network';");

                // Send the event to the event handler.
                instance.getTimers().getEventManager().event(player.getUniqueId().toString(), aEvent, message);
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

    public void updateCoordinateTransform(PlotSQL plotSQL, Location l) {
        dx = -plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + l.getWorld().getName() + "';");
        dz = -plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + l.getWorld().getName() + "';");
    }

    public Location getLocationWithCoordinateTransform() {
        return new Location(
                player.getWorld(),
                player.getLocation().getX() + dx,
                player.getLocation().getY(),
                player.getLocation().getZ() + dz,
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
