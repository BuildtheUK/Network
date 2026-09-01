package net.bteuk.network.gui.staff;

import net.bteuk.network.api.plotsystem.SubmittedPlot;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkMultiPageGui;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Material;

import java.util.Comparator;
import java.util.List;

/**
 * Shows all plots that can be reviewed by the user.
 */
public class PlotReviewGui extends NetworkMultiPageGui {

    private final NetworkUser user;

    private List<SubmittedPlot> submittedPlots;

    public PlotReviewGui(GuiProvider provider, NetworkUser user, List<SubmittedPlot> submittedPlots) {
        super(provider, 45, ChatUtils.title("Submitted Plots"));
        this.user = user;
        this.submittedPlots = submittedPlots;
    }

    @Override
    protected int getButtonCount() {
        return submittedPlots.size();
    }

    @Override
    protected void createPageButton(int slot, int index) {
        SubmittedPlot submittedPlot = submittedPlots.get(index);
        setItem(slot, Utils.createItem(getPlotDifficultyMaterial(submittedPlot.difficulty()), 1, Utils.title("Review Plot " + submittedPlot.id())),
                (NetworkUser user) -> reviewPlot(provider, submittedPlot, user));
    }

    @Override
    protected void addAdditionalButtons() {
        addReturnToLastSlot((NetworkUser user) -> {
            this.delete();
            user.staffGui = new StaffGui(provider, user);
            user.staffGui.open(user.player);
        });
    }

    /**
     * Updates the list of submitted plots.
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        submittedPlots = getSubmittedPlots(user, provider.plotSQL());
        super.refresh();
    }

    public static void reviewPlot(GuiProvider provider, SubmittedPlot submittedPlot, NetworkUser user) {
        String server = provider.plotSQL().getString(
                "SELECT server FROM location_data WHERE name = (" +
                        "SELECT location FROM plot_data WHERE id = " + submittedPlot.id() +
                        ");"
        );
        // If they are not in the same server as the plot, teleport them to that server and start the reviewing process.
        user.player.closeInventory();
        if (server.equals(provider.constants().serverName())) {
            provider.eventAPI().createEvent(user.getUuid(), provider.constants().serverName(), "review plot " + submittedPlot.id());
        } else {
            // Player is not on the current server.
            // Set the server join event.
            provider.eventAPI().createJoinEvent(user.getUuid(), "review plot " + submittedPlot.id());

            // Teleport them to the server.
            provider.serverAPI().switchServer(PlayerAdapter.adapt(user.player), server);
        }
    }

    /**
     * Gets a list of submitted plots that are reviewable by the given user sorted by submit time.
     *
     * @param user    the user that can review the plots
     * @param plotSQL plot database
     * @return list of submitted plots
     */
    public static List<SubmittedPlot> getSubmittedPlots(NetworkUser user, PlotSQL plotSQL) {
        boolean isArchitect = user.hasPermission("group.architect");
        boolean isReviewer = user.hasPermission("group.reviewer");

        List<SubmittedPlot> submittedPlots = plotSQL.getReviewablePlots(user.player.getUniqueId().toString(), isArchitect, isReviewer);
        submittedPlots.sort(Comparator.comparingLong(SubmittedPlot::submitTime));
        return submittedPlots;
    }

    private static Material getPlotDifficultyMaterial(int difficulty) {
        return switch (difficulty) {
            case 1 -> Material.LIME_CONCRETE;
            case 2 -> Material.YELLOW_CONCRETE;
            case 3 -> Material.RED_CONCRETE;
            default -> Material.STRUCTURE_VOID;
        };
    }
}
