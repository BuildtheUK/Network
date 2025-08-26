package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.gui.plotsystem.PlotInfo;
import net.bteuk.network.gui.plotsystem.PlotMenu;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.PlotStatus;
import net.bteuk.network.utils.plotsystem.ReviewFeedback;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@Log
public class Plot extends AbstractCommand {

    private final Network instance;
    private final EventAPI eventAPI;
    private final PlotSQL plotSQL;

    public Plot(Network instance, EventAPI eventAPI, PlotSQL plotSQL) {
        this.instance = instance;
        this.eventAPI = eventAPI;
        this.plotSQL = plotSQL;
        setTabCompleter(new FixedArgSelector(Arrays.asList("info", "join"), 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        if (args.length < 1) {
            error(player);
            return;
        }

        int plotID = -1;
        if (args.length > 1) {
            // Check if the plotID is an actual number.
            try {
                plotID = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                error(player);
                return;
            }
        }

        switch (args[0]) {
            case "menu" -> menu(player);
            case "info" -> info(player, plotID);
            case "join" -> join(player, plotID);
            case "feedback" -> feedback(player, plotID);
            default -> error(player);
        }
    }

    private void menu(Player p) {
        // Get the user.
        NetworkUser u = instance.getUser(p);
        if (u == null) {
            p.sendMessage(ChatUtils.error("An error occurred, please rejoin!"));
            log.severe("No user exists for player " + p.getName());
            return;
        }
        // Open the plot menu.
        if (u.mainGui != null) {
            u.mainGui.delete();
        }
        u.mainGui = new PlotMenu(u);
        u.mainGui.open(u);
    }

    private void info(Player p, int plot) {
        if (plot == -1) {
            error(p);
            return;
        }
        // Check if the plot exists and is not deleted.
        PlotStatus status = PlotStatus.fromDatabaseValue(plotSQL.getString("SELECT status FROM plot_data WHERE id=" + plot + ";"));
        if (status == null || status == PlotStatus.DELETED) {
            p.sendMessage(ChatUtils.error("This plot does not exist."));
            return;
        }
        // Get the user.
        NetworkUser u = instance.getUser(p);
        if (u == null) {
            p.sendMessage(ChatUtils.error("An error occurred, please rejoin!"));
            log.severe("No user exists for player " + p.getName());
            return;
        }
        // Open the plot info menu.
        if (u.mainGui != null) {
            u.mainGui.delete();
        }
        u.mainGui = new PlotInfo(u, plot);
        u.mainGui.open(u);
    }

    private void join(Player p, int plot) {
        if (plot == -1) {
            error(p);
            return;
        }

        // Check if they have an invitation for this plot.
        if (plotSQL.hasRow("SELECT id FROM plot_invites WHERE id=" + plot + " AND uuid='" + p.getUniqueId() + "';")) {

            // Add server event to join plot.
            eventAPI.createEvent(p.getUniqueId().toString(), "plotsystem",
                    plotSQL.getString("SELECT server FROM " + "location_data WHERE name='" + plotSQL.getString("SELECT location FROM plot_data WHERE id=" + plot + ";") + "';"),
                    "join plot " + plot);

            // Remove invite.
            plotSQL.update("DELETE FROM plot_invites WHERE id=" + plot + " AND uuid='" + p.getUniqueId() + "';");
        } else {
            p.sendMessage(ChatUtils.error("You have not been invited to join this plot."));
        }
    }

    private void feedback(Player player, int plot) {
        if (plot == -1) {
            error(player);
            return;
        }
        // Check if the player is the owner of a member of the plot.
        // Then open the latest feedback.
        // And set their Main gui to the plot info of this plot.
        if (!plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plot + " AND uuid='" + player.getUniqueId() + "';")) {
            player.sendMessage(ChatUtils.error("You are no longer the owner or a member of this plot."));
            return;
        }

        // Find the latest attempt.
        String uuid = plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plot + " AND is_owner=1;");
        int latestAttempt = plotSQL.getInt("SELECT MAX(attempt) FROM plot_review WHERE plot_id=" + plot + " AND " + "uuid='" + uuid + "' AND accepted=0 AND completed=1;");

        if (latestAttempt == 0) {
            player.sendMessage(Utils.error("There is no feedback available for this plot."));
            return;
        }

        NetworkUser user = instance.getUser(player);
        if (user != null) {
            user.mainGui = new PlotInfo(user, plot);
        }

        // Create book.
        int reviewId = plotSQL.getInt("SELECT id FROM plot_review WHERE plot_id=" + plot + " AND uuid='" + uuid + "' " + "AND attempt=" + latestAttempt + ";");

        // Open the book.
        player.openBook(ReviewFeedback.createFeedbackBook(reviewId));
    }

    private void error(Player p) {
        p.sendMessage(ChatUtils.error("/plot menu"));
        p.sendMessage(ChatUtils.error("/plot info <plotID>"));
        p.sendMessage(ChatUtils.error("/plot join <plotID>"));
        p.sendMessage(ChatUtils.error("/plot feedback <plotID>"));
    }

    @Override
    public String getLabel() {
        return "plot";
    }

    @Override
    public String getDescription() {
        return "Allows players to manipulate plots without using the gui.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("plots");
    }
}
