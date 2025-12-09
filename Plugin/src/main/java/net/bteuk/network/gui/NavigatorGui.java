package net.bteuk.network.gui;

import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.gui.navigation.ExploreGui;
import net.bteuk.network.gui.tutorials.TutorialsGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.LightsOut;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

public class NavigatorGui extends NetworkGui {

    public NavigatorGui(GuiProvider provider) {
        super(provider, 27, Component.text("Navigator", NamedTextColor.AQUA, TextDecoration.BOLD));

        Constants constants = provider.constants();
        GlobalSQL globalSQL = provider.globalSQL();
        Nightvision nightvision = provider.nightvision();
        Lobby lobby = provider.lobby();
        Back back = provider.back();
        EventAPI eventAPI = provider.eventAPI();
        ServerAPI serverAPI = provider.serverAPI();

        setItem(2, Utils.createItem(Material.DIAMOND_PICKAXE, 1, Utils.title("Build"), Utils.line("Click to open the build menu.")), (NetworkUser u) -> {
            // Switch to the build menu.
            u.mainGui = new BuildGui(provider, u);
            u.mainGui.open(u.player);
        });

        setItem(4, Utils.createItem(Material.SPRUCE_BOAT, 1, Utils.title("Explore"), Utils.line("Click to open the explore menu.")), (NetworkUser u) -> {

            // Click Action
            if (constants.warpsEnabled()) {
                u.mainGui = new ExploreGui(provider, u);
                u.mainGui.open(u.player);
            } else {
                u.player.closeInventory();
                u.player.sendMessage(ChatUtils.error("Warps are currently not enabled!"));
            }
        });

        setItem(6, Utils.createItem(Material.KNOWLEDGE_BOOK, 1, Utils.title("Tutorials"), Utils.line("Click to open the tutorials menu.")), (NetworkUser u) -> {
            // Switch to tutorials menu if it's online and enabled.
            // If the current server is already tutorials, don't open the gui.
            if (constants.serverType() == ServerType.TUTORIAL) {
                u.player.closeInventory();
                u.player.sendMessage(ChatUtils.error("You are already in the tutorials server, please use the" + " menu in slot 8."));
            } else if (constants.tutorials()) {
                if (globalSQL.hasRow("SELECT name FROM server_data WHERE " + "type='TUTORIAL' AND online=1;")) {

                    u.mainGui = new TutorialsGui(provider, u);
                    u.mainGui.open(u.player);
                } else {
                    u.player.closeInventory();
                    u.player.sendMessage(ChatUtils.error("The tutorials server is offline!"));
                }
            } else {
                u.player.closeInventory();
                u.player.sendMessage(ChatUtils.error("Tutorials are currently not enabled!"));
            }
        });

        setItem(26, Utils.createItem(Material.NETHER_STAR, 1, Utils.title("Toggle Navigator"), Utils.line("Click to toggle the navigator in your inventory."),
                Utils.line("You can always open this menu with ").append(Component.text("/navigator", NamedTextColor.GRAY))), (NetworkUser u) -> {

            if (u.isNavigatorEnabled()) {

                // Set navigator to false and remove the navigator from the inventory.
                u.setNavigatorEnabled(false);
                u.player.getInventory().setItem(8, null);

                // Disable navigator in database.
                globalSQL.update("UPDATE player_data SET navigator=0 WHERE uuid='" + u.player.getUniqueId() + "';");

                u.player.sendMessage(ChatUtils.success("Disabled navigator in inventory."));
            } else {

                // Set navigator to true.
                u.setNavigatorEnabled(true);

                // Enable navigator in database.
                globalSQL.update("UPDATE player_data SET navigator=1 WHERE uuid='" + u.player.getUniqueId() + "';");

                u.player.sendMessage(ChatUtils.success("Enabled navigator in inventory."));
            }
        });

        setItem(25, Utils.createPotion(Material.SPLASH_POTION, PotionEffectType.NIGHT_VISION, 1, Utils.title("Toggle Nightvision"), Utils.line("Click to toggle nightvision."),
                Utils.line("You can also use the command ").append(Component.text("/nightvision", NamedTextColor.GRAY)).append(Utils.line(" or "))
                        .append(Component.text("/nv", NamedTextColor.GRAY))), nightvision::toggleNightvision);

        setItem(19, Utils.createItem(Material.REDSTONE_LAMP, 1, Utils.title("Lights Out"), Utils.line("Play a game of Lights Out.")), (NetworkUser u) -> {
            if (u.lightsOut == null) {

                u.lightsOut = new LightsOut(provider, u);
                u.lightsOut.open(u.player);
            } else {

                u.lightsOut.open(u.player);
            }
        });

        // Set rules.
        setItem(21, Utils.createItem(Material.ENCHANTED_BOOK, 1, Utils.title("Rules"), Utils.line("Click to view the rules.")), (NetworkUser u) -> {
            u.player.closeInventory();
            u.player.openBook(lobby.getRules());
        });

        // Spawn
        if (!constants.standalone()) {
            setItem(23, Utils.createItem(Material.RED_BED, 1, Utils.title("Spawn"), Utils.line("Teleport to spawn.")), (NetworkUser u) -> {
                u.player.closeInventory();

                // If server is Lobby, teleport to spawn.
                NetworkLocation location = LocationAdapter.adapt(u.player.getLocation());
                if (constants.serverType() == ServerType.LOBBY) {

                    back.setPreviousCoordinate(u.player.getUniqueId().toString(), location);
                    u.player.teleport(lobby.getSpawn());
                    u.player.sendMessage(ChatUtils.success("Teleported to spawn."));
                } else {

                    // Set teleport event to go to spawn.
                    u.player.closeInventory();
                    eventAPI.createTeleportEvent(true, u.player.getUniqueId().toString(), "teleport spawn", location);
                    serverAPI.switchServer(PlayerAdapter.adapt(u.player), globalSQL.getString("SELECT " + "name FROM server_data WHERE type='LOBBY';"));
                }
            });
        }
    }
}
