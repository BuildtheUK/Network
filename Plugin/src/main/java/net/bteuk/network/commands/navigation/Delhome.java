package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.HomeSelector;
import net.bteuk.network.sql.GlobalSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Delhome extends AbstractCommand {

    private final GlobalSQL globalSQL;

    // Constructor to enable the command.
    public Delhome(Network instance) {
        this.globalSQL = instance.getGlobalSQL();

        // Set tab completer.
        setTabCompleter(new HomeSelector(globalSQL));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // If no args set default home.
        // Else try to set homes with specific names.
        // For multiple homes the player needs permission.
        if (args.length == 0) {

            // Check if the default home exists.
            if (!globalSQL.hasRow("SELECT uuid FROM home WHERE uuid='" + player.getUniqueId() + "' AND name IS NULL;")) {
                player.sendMessage(ChatUtils.error("You not have a default home set, set one with ").append(Component.text("/sethome", NamedTextColor.DARK_RED)));
                return;
            }

            // Get coordinate ID.
            int coordinate_id = globalSQL.getInt("SELECT coordinate_id FROM home WHERE uuid='" + player.getUniqueId() + "' AND " + "name IS NULL;");

            // Delete default home.
            globalSQL.update("DELETE FROM home WHERE uuid='" + player.getUniqueId() + "' AND name IS NULL;");
            player.sendMessage(ChatUtils.success("Default home removed."));

            // Delete coordinate id.
            globalSQL.update("DELETE FROM coordinates WHERE id=" + coordinate_id + ";");
        } else {

            // Check for permission.
            if (!player.hasPermission("uknet.navigation.homes")) {
                player.sendMessage(ChatUtils.error("You do not have permission to delete multiple homes, only to " + "delete a default home using ")
                        .append(Component.text("/delhome", NamedTextColor.DARK_RED)));
                return;
            }

            // Get the name.
            String name = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

            // Get coordinate ID.
            int coordinate_id = globalSQL.getInt("SELECT coordinate_id FROM home WHERE uuid='" + player.getUniqueId() + "' AND " + "name='" + name + "';");

            // Check is home with this name exists.
            if (!globalSQL.hasRow("SELECT uuid FROM home WHERE uuid='" + player.getUniqueId() + "' AND name='" + name + "';")) {
                player.sendMessage(ChatUtils.error("You do not have a home set with the name ").append(Component.text(name, NamedTextColor.DARK_RED)));
                return;
            }

            // Delete home
            globalSQL.update("DELETE FROM home WHERE uuid='" + player.getUniqueId() + "' AND name='" + name + "';");
            player.sendMessage(Component.text(name, NamedTextColor.DARK_AQUA).append(ChatUtils.success(" home removed.")));

            // Delete coordinate id.
            globalSQL.update("DELETE FROM coordinates WHERE id=" + coordinate_id + ";");
        }
    }

    @Override
    public String getLabel() {
        return "delhome";
    }

    @Override
    public String getDescription() {
        return "Delete a home.";
    }
}
