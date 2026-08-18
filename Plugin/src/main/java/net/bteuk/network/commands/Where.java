package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.sql.PlotSQL;
import net.buildtheearth.terraminusminus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraminusminus.projection.OutOfProjectionBoundsException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;

import static net.bteuk.network.core.ServerType.PLOT;

@Log
public class Where extends AbstractCommand {

    private static final DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("##.#####");
    private final EarthGeneratorSettings bteGeneratorSettings =
            EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS);
    private final PlotSQL plotSQL;
    private final PlotAPI plotAPI;
    private final Constants constants;

    public Where(PlotSQL plotSQL, PlotAPI plotAPI, Constants constants) {
        this.plotSQL = plotSQL;
        this.plotAPI = plotAPI;
        this.constants = constants;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        boolean bPlotWorld = (constants.serverType() == PLOT || constants.standalone()) && plotAPI.hasLocation(player.getWorld().key().asMinimalString());

        if (!bPlotWorld && !(constants.serverType() == ServerType.EARTH && player.getWorld().key().asMinimalString().equals(constants.earthDimension()))) {
            player.sendMessage(ChatUtils.error("This world does not support coordinates."));
            return;
        }

        // Send coordinates with a link to Google Maps to the player.
        try {
            int deltaX = 0;
            int deltaZ = 0;
            if (bPlotWorld) {
                // Get negative coordinate transform of new location.
                deltaX = -plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + player.getWorld().key().asMinimalString() + "';");
                deltaZ = -plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + player.getWorld().key().asMinimalString() + "';");
            }

            double[] coords = bteGeneratorSettings.projection().toGeo(player.getLocation().getX() + deltaX,
                    player.getLocation().getZ() + deltaZ);

            player.sendMessage(ChatUtils.success("Your coordinates are ")
                    .append(Component.text(DECIMAL_FORMATTER.format(coords[1]), NamedTextColor.DARK_AQUA))
                    .append(ChatUtils.success(","))
                    .append(Component.text(DECIMAL_FORMATTER.format(coords[0]), NamedTextColor.DARK_AQUA)));
            Component message = ChatUtils.success("Click here to view the coordinates in Google Maps.");
            message = message.clickEvent(ClickEvent.openUrl("https://www.google.com/maps/@?api=1&map_action=map&basemap=satellite&zoom=21&center=" + coords[1] + "," + coords[0]));
            player.sendMessage(message);
        } catch (OutOfProjectionBoundsException e) {
            player.sendMessage(ChatUtils.error("You are not standing in a location where coordinates can be retrieved" +
                    "."));
        }
    }

    @Override
    public String getLabel() {
        return "where";
    }

    @Override
    public String getDescription() {
        return "Returns the coordinates where the player is standing with a link to google maps.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("location", "ll");
    }
}
