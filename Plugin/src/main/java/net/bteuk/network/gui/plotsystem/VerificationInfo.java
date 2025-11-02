package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.api.plotsystem.ReviewFeedback;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

public class VerificationInfo extends NetworkRefreshableGui {

    private final int verificationId;

    private final PlotSQL plotSQL;
    private final GlobalSQL globalSQL;

    public VerificationInfo(GuiProvider provider, int verificationId) {

        // Create the menu.
        super(provider, 27, Component.text("Verification " + verificationId, NamedTextColor.AQUA, TextDecoration.BOLD));

        this.verificationId = verificationId;

        this.plotSQL = provider.plotSQL();
        this.globalSQL = provider.globalSQL();
    }

    protected void createGui() {

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the plot menu.")),
                (NetworkUser u) -> {
                    this.delete();
                    u.mainGui = new PlotMenu(provider, u);
                    u.mainGui.open(u.player);
                }
        );

        String verifierUuid =
                plotSQL.getString("SELECT verifier FROM plot_verification WHERE id=" + verificationId + ";");
        String verifier = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + verifierUuid + "';");

        int plotId = plotSQL.getInt("SELECT plot_id FROM plot_review WHERE id=(SELECT review_id FROM " +
                "plot_verification WHERE id=" + verificationId + ");");
        boolean feedbackChanged =
                plotSQL.hasRow("SELECT 1 FROM plot_verification_category WHERE verification_id=" + verificationId +
                        " AND book_id_old <> book_id_new;");
        boolean selectionChanged =
                plotSQL.hasRow("SELECT 1 FROM plot_verification_category WHERE verification_id=" + verificationId +
                        " AND selection_old <> selection_old;");

        String outcomeOld =
                plotSQL.getBoolean("SELECT accepted_old FROM plot_verification WHERE id=" + verificationId + ";") ?
                        "Accepted" : "Denied";
        String outcomeNew =
                plotSQL.getBoolean("SELECT accepted_new FROM plot_verification WHERE id=" + verificationId + ";") ?
                        "Accepted" : "Denied";

        Component[] description;
        if (!outcomeOld.equals(outcomeNew)) {
            description = new Component[]{
                    Utils.line("Verified by " + verifier),
                    Utils.line("The outcome of the review"),
                    Utils.line("was altered from:"),
                    Utils.line(outcomeOld + " -> " + outcomeNew)
            };
        } else if (selectionChanged) {
            description = new Component[]{
                    Utils.line("Verified by " + verifier),
                    Utils.line("The selection of at least one"),
                    Utils.line("category was altered, check"),
                    Utils.line("the books to see the changes.")
            };
        } else if (feedbackChanged) {
            description = new Component[]{
                    Utils.line("Verified by " + verifier),
                    Utils.line("The feedback of at least one"),
                    Utils.line("category was altered, check"),
                    Utils.line("the books to see the changes.")
            };
        } else {
            description = new Component[]{
                    Utils.line("Verified by " + verifier)
            };
        }

        // Verification Info
        setItem(4, Utils.createItem(Material.BOOK, 1,
                Utils.title("Plot " + plotId),
                description));

        // If the selection or feedback was changed show the before and after books.
        setItem(20, Utils.createItem(Material.WRITABLE_BOOK, 1,
                        Utils.title("Initial Feedback"),
                        Utils.line("Click to show initial feedback"),
                        Utils.line("for categories that were"),
                        Utils.line("altered by the verifier.")),
                (NetworkUser u) -> {
                    // Open the feedback book.
                    u.player.openBook(ReviewFeedback.createVerificationFeedbackBook(plotSQL, verificationId, true));
                });

        // If the selection or feedback was changed show the before and after books.
        setItem(24, Utils.createItem(Material.WRITABLE_BOOK, 1,
                        Utils.title("Altered Feedback"),
                        Utils.line("Click to show altered feedback"),
                        Utils.line("for categories that were"),
                        Utils.line("altered by the verifier.")),
                (NetworkUser u) -> {
                    // Open the feedback book.
                    u.player.openBook(ReviewFeedback.createVerificationFeedbackBook(plotSQL, verificationId, false));
                });

        // Teleport to the plot.
        setItem(22, Utils.createItem(Material.ENDER_PEARL, 1,
                        Utils.title("Teleport to Plot"),
                        Utils.line("Click to teleport to this plot.")),
                (NetworkUser u) -> {
                    u.player.closeInventory();

                    // Get the server of the plot.
                    String server = plotSQL.getString("SELECT server FROM location_data WHERE name='"
                            + plotSQL.getString("SELECT location FROM plot_data WHERE id=" + plotId + ";")
                            + "';");

                    // If the plot is on the current server teleport them directly.
                    // Else teleport them to the correct server and them teleport them to the plot.
                    NetworkLocation location = LocationAdapter.adapt(u.player.getLocation());
                    if (server.equals(provider.constants().serverName())) {
                        provider.eventAPI().createTeleportEvent(false, u.player.getUniqueId().toString(), "plotsystemteleport plot " + plotId, location);
                    } else {
                        u.player.closeInventory();

                        // Set the server join event.
                        provider.eventAPI().createTeleportEvent(true, u.player.getUniqueId().toString(), "plotsystemteleport plot " + plotId, location);

                        // Teleport them to another server.
                        provider.serverAPI().switchServer(PlayerAdapter.adapt(u.player), server);
                    }
                });

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the verified review menu.")),
                (NetworkUser u) -> {
                    // Delete this gui.
                    this.delete();
                    u.mainGui = null;

                    // Switch to verified review menu.
                    u.mainGui = new VerificationMenu(provider, u);
                    u.mainGui.open(u.player);
                });
    }
}

