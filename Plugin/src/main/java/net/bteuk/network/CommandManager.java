package net.bteuk.network;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.commands.BuildingCompanionCommand;
import net.bteuk.network.commands.Buildings;
import net.bteuk.network.commands.Clear;
import net.bteuk.network.commands.Demote;
import net.bteuk.network.commands.Discord;
import net.bteuk.network.commands.Focus;
import net.bteuk.network.commands.Gamemode;
import net.bteuk.network.commands.Hdb;
import net.bteuk.network.commands.Help;
import net.bteuk.network.commands.Me;
import net.bteuk.network.commands.Msg;
import net.bteuk.network.commands.Navigator;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.commands.Phead;
import net.bteuk.network.commands.Plot;
import net.bteuk.network.commands.Pmute;
import net.bteuk.network.commands.ProgressMap;
import net.bteuk.network.commands.Promote;
import net.bteuk.network.commands.Ptime;
import net.bteuk.network.commands.Punmute;
import net.bteuk.network.commands.Pweather;
import net.bteuk.network.commands.RegionCommand;
import net.bteuk.network.commands.Reply;
import net.bteuk.network.commands.Rules;
import net.bteuk.network.commands.Season;
import net.bteuk.network.commands.Speed;
import net.bteuk.network.commands.TipsToggle;
import net.bteuk.network.commands.Where;
import net.bteuk.network.commands.Zone;
import net.bteuk.network.commands.give.GiveBarrier;
import net.bteuk.network.commands.give.GiveDebugStick;
import net.bteuk.network.commands.give.GiveLight;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.commands.navigation.Delhome;
import net.bteuk.network.commands.navigation.Home;
import net.bteuk.network.commands.navigation.Homes;
import net.bteuk.network.commands.navigation.Navigation;
import net.bteuk.network.commands.navigation.Server;
import net.bteuk.network.commands.navigation.Sethome;
import net.bteuk.network.commands.navigation.Spawn;
import net.bteuk.network.commands.navigation.Tp;
import net.bteuk.network.commands.navigation.TpToggle;
import net.bteuk.network.commands.navigation.Warp;
import net.bteuk.network.commands.navigation.Warps;
import net.bteuk.network.commands.staff.Exp;
import net.bteuk.network.commands.staff.Staff;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lobby.LobbyCommand;
import net.bteuk.network.regions.RegionManager;
import org.bukkit.plugin.Plugin;

import java.util.List;

import static net.bteuk.network.utils.NetworkConfig.CONFIG;

public class CommandManager {

    public static void registerCommands(Network instance, Constants constants, EventAPI eventAPI, Afk afk, RegionManager regionManager) {

        LifecycleEventManager<Plugin> manager = instance.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            /*
             * Navigation commands.
             */
            if (constants.tpllEnabled()) {
                commands.register("tpll", "Teleport to coordinates", instance.getTpll());
            }
            if (constants.ll()) {
                commands.register("where", "Returns the coordinates where the player is standing with a link to google maps.", List.of("location", "ll"), new Where(instance));
            }
            commands.register("teleport", "Teleport to any online player.", List.of("tp"), new Tp());
            commands.register("back", "Teleports the player to the previous teleported location.", new Back(eventAPI));
            commands.register("warp", "Warp to locations in the exploration menu.", new Warp());
            commands.register("warps", "List all warps on the server, 16 per page.", new Warps());
            commands.register("navigation", "Adds commands to do with navigation.", new Navigation());
            if (!constants.standalone()) {
                commands.register("lobby", "Command for all lobby management.", new LobbyCommand(instance));
                commands.register("spawn", "Teleport to spawnpoint in lobby.", new Spawn());
                commands.register("server", "Switch server by command.", new Server());
            }
            if (CONFIG.getBoolean("homes.enabled")) {
                commands.register("sethome", "Set a home to your current location.", new Sethome(instance));
                commands.register("home", "Teleport to your home.", new Home(instance));
                commands.register("delhome", "Delete a home.", new Delhome(instance));
                commands.register("homes", "Like warps, but for homes, shows all homes the player has set.", new Homes());
            }

            /*
             * Gui commands.
             */
            commands.register("navigator", "Opens the main gui, will always return to the previous menu if possible.", List.of("nav", "gui", "menu", "claim"), new Navigator());
            if (constants.plotSystemEnabled()) {
                commands.register("plot", "Allows players to manipulate plots without using the gui.", List.of("plots"), new Plot(instance));
                commands.register("zone", "Zone command.", new Zone());
            }
            if (constants.regionsEnabled()) {
                commands.register("region", "Allows players to manipulate regions without using the gui.", new RegionCommand());
            }

            /*
             * Staff commands.
             */
            commands.register("staff", "Opens the Staff Menu.", List.of("st"), new Staff());
            if (CONFIG.getBoolean("staff.moderation.enabled")) {
                commands.register("ban", "Bans a player for a specific duration and reason.", instance.getBan());
                commands.register("mute", "Mutes a player for a specific duration and reason.", instance.getMute());
                commands.register("kick", "Kick a player for the server.", instance.getKick());
                commands.register("unban", "Unban a previously banned player.", instance.getUnban());
                commands.register("unmute", "Unmute a previously muted player.", instance.getUnmute());
            }

            /*
             * Utility commands.
             */
            commands.register("building", "adds or shows completed buildings", new Buildings(instance));
            commands.register("teleporttoggle", "Enables/Disables the ability for other players to teleport to you.", List.of("tptoggle", "toggleteleport", "toggletp"), new TpToggle());
            if (!constants.standalone()) {
                commands.register("discord", "Sends a link to our discord server.", new Discord());
                commands.register("focus", "Toggle focus mode, hides chat and players.", List.of("focusmode", "fm"), new Focus());
            }
            commands.register("nightvision", "Toggle nightvision.", List.of("nv"), new Nightvision());
            commands.register("speed", "Sets the players speed, value up to 10.", new Speed());
            commands.register("help", "Help menu for information on commands and server features.", new Help());
            commands.register("afk", "Toggles afk status.", afk);
            commands.register("rules", "Get rules book.", new Rules());
            commands.register("clear", "Clears your inventory.", new Clear());
            commands.register("debugstick", "Get the debug stick.", new GiveDebugStick(instance));
            commands.register("light", "Get a light block.", new GiveLight(instance));
            commands.register("barrier", "Get a barrier block.", new GiveBarrier(instance));
            commands.register("gamemode", "Switch gamemode.", List.of("gm"), new Gamemode());
            commands.register("phead", "Get the player head of someone who has connected to the server.", new Phead());
            commands.register("hdb", "Added so it can be routed to /skulls", new Hdb());
            if (constants.progressMap()) {
                commands.register("progressmap", "Sends a link of the progress map", List.of("progress"), new ProgressMap());
            }
            if (constants.tips()) {
                commands.register("tips", "Toggles tips in chat.", List.of("toggletips", "tipstoggle"), new TipsToggle());
            }
            commands.register("ptime", "Sets the time of day for the player", new Ptime());
            commands.register("pweather", "Sets the weather for the player", new Pweather());
            commands.register("season", "Command for creating, starting and ending seasons.", List.of("seasons"), new Season());
            commands.register("exp", "Test command for adding exp.", new Exp());
            commands.register("buildingcompanion", "Toggle the building companion.", List.of("bc", "companion"), new BuildingCompanionCommand(instance, constants, regionManager));
            commands.register("pmute", "Mute a player", new Pmute(instance));
            commands.register("punmute", "Unmute a player", new Punmute(instance));
            Msg msgCommand = new Msg(instance);
            commands.register("msg", "Sends a direct message to a player.", msgCommand);
            commands.register("w", "Sends a direct message to a player.", msgCommand);
            commands.register("tell", "Sends a direct message to a player.", msgCommand);
            commands.register("r","sends a direct message to the last player you messaged", List.of("reply"), new Reply(msgCommand));
            commands.register("promote", "Add a role to a player.", new Promote(instance));
            commands.register("demote", "Remove a role from a player.", new Demote(instance));
            commands.register("me", "Disabled", new Me());

            // commands.register("bteuk", "Test", new BTEUK());
        });
    }
}
