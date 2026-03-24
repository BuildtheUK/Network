package net.bteuk.network.gui.staff;

import net.bteuk.network.api.plotsystem.SubmittedPlot;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkMultiPageGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import org.bukkit.Material;

import java.util.List;

/**
 * Shows all plots that can be reviewed by the user.
 */
public class PlotReviewGui extends NetworkMultiPageGui {

    private final List<SubmittedPlot> submittedPlots;

    public PlotReviewGui(GuiProvider provider, List<SubmittedPlot> submittedPlots) {
        super(provider, 45, ChatUtils.title("Submitted Plots"), submittedPlots.size());
        this.submittedPlots = submittedPlots;
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

    public static void reviewPlot(GuiProvider provider, SubmittedPlot submittedPlot, NetworkUser user) {
        String server = provider.plotSQL().getString("SELECT server FROM " + "location_data WHERE name='" + provider.plotSQL()
                .getString("SELECT location FROM plot_data WHERE " + "id=" + submittedPlot.id() + ";") + "';");
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

    private static Material getPlotDifficultyMaterial(int difficulty) {
        return switch (difficulty) {
            case 1 -> Material.LIME_CONCRETE;
            case 2 -> Material.YELLOW_CONCRETE;
            case 3 -> Material.RED_CONCRETE;
            default -> Material.STRUCTURE_VOID;
        };
    }
}
