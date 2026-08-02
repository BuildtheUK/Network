package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.commands.AbstractCommand;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Sethome extends AbstractCommand {

    private final GlobalSQL globalSQL;

    // Constructor to enable the command.
    public Sethome(Network instance) {
        this.globalSQL = instance.getGlobalSQL();
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

            // If already has a default home set, the player first has to delete it.
            if (globalSQL.hasRow("SELECT uuid FROM home WHERE uuid='" + player.getUniqueId() + "' AND name IS NULL;")) {
                player.sendMessage(ChatUtils.error("You already have a default home set, to delete it type ")
                        .append(Component.text("/delhome", NamedTextColor.DARK_RED)));
                return;
            }

            // Set home to the current location.
            int coordinate_id = getCoordinateID(player.getLocation());

            globalSQL.update(
                    "INSERT INTO home(coordinate_id,uuid) VALUES(" + coordinate_id + ",'" + player.getUniqueId() +
                            "');");

            player.sendMessage(ChatUtils.success("Default home set to current location, to teleport here type ")
                    .append(Component.text("/home", NamedTextColor.DARK_AQUA)));
        } else {

            // Check for permission.
            if (!player.hasPermission("uknet.navigation.homes")) {
                player.sendMessage(ChatUtils.error("You do not have permission to set multiple homes, only to set a " +
                                "default home using ")
                        .append(Component.text("/sethome", NamedTextColor.DARK_RED)));
                return;
            }

            // Check if a home with this name already exists.
            String name = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

            // Check name length.
            if (name.length() > 64) {
                player.sendMessage(ChatUtils.error("The home name must be 64 characters or less."));
                return;
            }

            if (globalSQL.hasRow(
                    "SELECT uuid FROM home WHERE uuid='" + player.getUniqueId() + "' AND name='" + name + "';")) {
                player.sendMessage(ChatUtils.error("You already have a home set with the name ")
                        .append(Component.text(name, NamedTextColor.DARK_RED))
                        .append(ChatUtils.error(", you can delete it by typing "))
                        .append(Component.text("/delhome " + name, NamedTextColor.DARK_RED)));
                return;
            }

            // Set home to current location.
            int coordinate_id = getCoordinateID(player.getLocation());

            globalSQL.update(
                    "INSERT INTO home(coordinate_id,uuid,name) VALUES(" + coordinate_id + ",'" + player.getUniqueId() + "','" + name + "');");

            player.sendMessage(ChatUtils.success("Home set to current location, to teleport here type ")
                    .append(Component.text("/home " + name, NamedTextColor.DARK_AQUA)));
        }
    }

    private int getCoordinateID(Location l) {
        // Create location coordinate.
        return globalSQL.addCoordinate(l);
    }

    @Override
    public String getLabel() {
        return "sethome";
    }

    @Override
    public String getDescription() {
        return "Set a home to your current location.";
    }
}
