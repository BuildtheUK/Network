package net.bteuk.network.proxy;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.bteuk.network.lib.dto.TabPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.btuk.proxy.core.chat.ChatHandler;
import org.btuk.proxy.core.config.Config;
import org.btuk.proxy.core.player.Player;
import org.btuk.proxy.core.scheduler.Scheduler;
import org.btuk.proxy.core.tab.AbstractTabManager;
import org.btuk.proxy.core.user.CoreUserManager;
import org.btuk.proxy.core.user.User;
import org.bukkit.Server;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction.ADD_PLAYER;
import static com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME;
import static com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction.UPDATE_LATENCY;
import static com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction.UPDATE_LISTED;

public class NetworkTabManager extends AbstractTabManager {

    private final ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

    private final Server server;

    public NetworkTabManager(Server server, Config config, CoreUserManager coreUserManager, ChatHandler chatHandler, Scheduler scheduler) {
        super(config, coreUserManager, chatHandler, scheduler);
        this.server = server;
    }

    @Override
    protected void addPlayerToTabList(Player player, User user, TabPlayer tabPlayer) {
        org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(player.getUniqueId().toString());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }
        sendPlayerInfoAddOrFullUpdate(bukkitPlayer, List.of(toFakeInfoData(user, tabPlayer)));
    }

    @Override
    protected void removePlayerFromTabList(Player player, TabPlayer tabPlayer) {
        org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(player.getUniqueId().toString());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }
        UUID fakeUuid = fakeUuidForRealUuid(tabPlayer.getUuid());
        sendPlayerInfoRemove(bukkitPlayer, List.of(fakeUuid));
    }

    @Override
    protected void updatePlayerPing(String name, int ping) {
        Optional<TabPlayer> tabPlayer = tabPlayers.stream().filter(player -> player.getName().equals(name)).findFirst();
        User user = coreUserManager.getUserByName(name);
        if (user == null) {
            return;
        }
        List<PlayerInfoData> updates = tabPlayer.stream().map(player -> toFakeInfoData(user, player)).toList();
        broadcastPlayerInfoUpdate(updates, EnumSet.of(UPDATE_LATENCY));
    }

    @Override
    protected void updatePlayerDisplayName(String name, TabPlayer updated) {
        User user = coreUserManager.getUserByName(name);
        if (user == null) {
            return;
        }
        List<PlayerInfoData> updates = List.of(toFakeInfoData(user, updated));
        broadcastPlayerInfoUpdate(updates, EnumSet.of(UPDATE_DISPLAY_NAME));
    }

    @Override
    protected int findPingForPlayer(String uuid) {
        org.bukkit.entity.Player bukkitPlayer = server.getPlayer(UUID.fromString(uuid));
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return -1;
        } else {
            return bukkitPlayer.getPing();
        }
    }

    @Override
    protected void updatePing() {
        List<PlayerInfoData> all = tabPlayers.stream().map(player -> {
            player.setPing(findPingForPlayer(player.getUuid()));
            User user = coreUserManager.getUserByUuid(player.getUuid());
            if (user == null) {
                return null;
            }
            return toFakeInfoData(user, player);
        }).filter(Objects::nonNull).toList();
        broadcastPlayerInfoUpdate(all, EnumSet.of(UPDATE_LATENCY));
    }

    /**
     * Update a specific user in the tablist of another user.
     * This can be used specifically when you do a personal mute of a player.
     *
     * @param user         the user to update the tablist for
     * @param userToUpdate the user to update in the tablist
     */
    @Override
    public void updatePlayerInTablistOfPlayer(User user, User userToUpdate) {
        org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(user.getUuid());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }
        TabPlayer tabPlayer = findTabPlayerByUuid(userToUpdate.getUuid());
        if (tabPlayer == null) {
            return;
        }
        // Update just that one entry in just that one viewer
        sendPlayerInfoAddOrFullUpdate(bukkitPlayer, List.of(toFakeInfoData(userToUpdate, tabPlayer)));
    }

    /**
     * Send the full tablist to a user.
     * This is used when a user connects to a server.
     * Adjust display names for muted players.
     */
    @Override
    public void sendTablist(User user) {
        org.bukkit.entity.Player bukkitPlayer = resolveBukkitPlayer(user.getUuid());
        if (bukkitPlayer == null || !bukkitPlayer.isOnline()) {
            return;
        }
        List<PlayerInfoData> allInfo = tabPlayers.stream().map(player -> {
            User tabUser = coreUserManager.getUserByUuid(player.getUuid());
            if (tabUser == null) {
                return null;
            }
            return toFakeInfoData(tabUser, player);
        }).filter(Objects::nonNull).toList();
        sendPlayerInfoAddOrFullUpdate(bukkitPlayer, allInfo);
        server.sendPlayerListHeaderAndFooter(HEADER, FOOTER);
    }

    private void broadcastPlayerInfoUpdate(List<PlayerInfoData> infoData, EnumSet<com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction> actions) {
        if (infoData == null || infoData.isEmpty()) return;

        for (org.bukkit.entity.Player viewer : server.getOnlinePlayers()) {
            sendPlayerInfoUpdate(viewer, infoData, actions);
        }
    }

    private void sendPlayerInfoAddOrFullUpdate(org.bukkit.entity.Player player, List<PlayerInfoData> infoData) {
        sendPlayerInfoUpdate(player, infoData, EnumSet.of(ADD_PLAYER, UPDATE_LISTED, UPDATE_LATENCY, UPDATE_DISPLAY_NAME));
    }

    private void sendPlayerInfoUpdate(org.bukkit.entity.Player viewer,
                                      List<PlayerInfoData> infoData,
                                      EnumSet<com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction> actions) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, actions);
        packet.getPlayerInfoDataLists().write(1, infoData);

        protocolManager.sendServerPacket(viewer, packet);
    }

    private void sendPlayerInfoRemove(org.bukkit.entity.Player viewer, List<UUID> uuids) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
        packet.getUUIDLists().write(0, uuids);
        protocolManager.sendServerPacket(viewer, packet);
    }

    private PlayerInfoData toFakeInfoData(User user, TabPlayer tabPlayer) {
        String realUuid = tabPlayer.getUuid();
        UUID fakeUuid = fakeUuidForRealUuid(realUuid);

        WrappedGameProfile profile = new WrappedGameProfile(fakeUuid, null);
        EnumWrappers.NativeGameMode gameMode = EnumWrappers.NativeGameMode.CREATIVE;

        Component displayName = formattedName(user, tabPlayer);
        WrappedChatComponent wrappedDisplayName = toWrappedChat(displayName);

        return new PlayerInfoData(fakeUuid, tabPlayer.getPing(), true, gameMode, profile, wrappedDisplayName);
    }

    private UUID fakeUuidForRealUuid(String realUuidString) {
        return UUID.nameUUIDFromBytes(("network-tab:" + realUuidString).getBytes(StandardCharsets.UTF_8));
    }

    private static WrappedChatComponent toWrappedChat(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        return WrappedChatComponent.fromJson(json);
    }

    private @Nullable org.bukkit.entity.Player resolveBukkitPlayer(String uuid) {
        return server.getOnlinePlayers().stream().filter(player -> player.getUniqueId().toString().equals(uuid)).findFirst().orElse(null);
    }
}
