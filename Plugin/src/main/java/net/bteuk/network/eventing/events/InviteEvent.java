package net.bteuk.network.eventing.events;

import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.SQLAPI;
import net.bteuk.network.core.Event;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class InviteEvent implements Event {

    private final SQLAPI globalSQL;
    private final PlotAPI plotAPI;
    private final RegionManager regionManager;

    public InviteEvent(SQLAPI globalSQL, PlotAPI plotAPI, RegionManager regionManager) {
        this.globalSQL = globalSQL;
        this.plotAPI = plotAPI;
        this.regionManager = regionManager;
    }

    @Override
    public void event(String uuid, String[] event, String sMessage) {

        switch (event[1]) {
            case "plot" -> {

                // Get player.
                Player p = Bukkit.getPlayer(UUID.fromString(uuid));

                // Send the player a message telling them the command to join the plot.
                if (p != null) {

                    int id = Integer.parseInt(event[2]);

                    p.sendMessage(ChatUtils.success("You have been invited to plot ").append(Component.text(event[2], NamedTextColor.DARK_AQUA)).append(ChatUtils.success(" by "))
                            .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + plotAPI.getPlotOwner(id) + "';"), NamedTextColor.DARK_AQUA)));

                    Component message = ChatUtils.success("To join the plot click here!");
                    message = message.clickEvent(ClickEvent.runCommand("/plot join " + event[2]));
                    p.sendMessage(message);
                }
            }
            case "zone" -> {

                // Get player.
                Player p = Bukkit.getPlayer(UUID.fromString(uuid));

                // Send the player a message telling them the command to join the plot.
                if (p != null) {
                    int id = Integer.parseInt(event[2]);

                    p.sendMessage(ChatUtils.success("You have been invited to zone ").append(Component.text(event[2], NamedTextColor.DARK_AQUA)).append(ChatUtils.success(" by "))
                            .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + plotAPI.getZoneOwner(id) + "';"), NamedTextColor.DARK_AQUA)));

                    Component message = ChatUtils.success("To join the zone click here!");
                    message = message.clickEvent(ClickEvent.runCommand("/zone join " + event[2]));
                    p.sendMessage(message);
                }
            }
            case "region" -> {
                // Get player.
                Player p = Bukkit.getPlayer(UUID.fromString(uuid));

                // Send the player a message telling them the command to join the plot.
                if (p != null) {

                    Region region = regionManager.getRegion(event[2]);

                    p.sendMessage(ChatUtils.success("You have been invited to region ").append(Component.text(event[2], NamedTextColor.DARK_AQUA)).append(ChatUtils.success(" by "))
                            .append(Component.text(globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + regionManager.getOwner(region) + "';"),
                                    NamedTextColor.DARK_AQUA)));

                    Component message = ChatUtils.success("To join the region click here!");
                    message = message.clickEvent(ClickEvent.runCommand("/region join " + event[2]));
                    p.sendMessage(message);
                }
            }
        }
    }
}
