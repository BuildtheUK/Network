package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.NavigationTabCompleter;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.navigation.AddLocation;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.enums.AddLocationType;
import net.bteuk.network.utils.enums.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@Log
public class Navigation extends AbstractCommand {

    private static final Component ERROR_SUBCATEGORY_ADD = ChatUtils.error("/navigation subcategory add [category] " +
            "<subcategory>");
    private static final Component ERROR_SUBCATEGORY_REMOVE = ChatUtils.error("/navigation subcategory remove " +
            "<subcategory>");

    private final Network instance;
    private final GlobalSQL globalSQL;
    private final GuiProvider provider;

    public Navigation(Network instance, GuiProvider provider) {
        this.instance = instance;
        this.globalSQL = instance.getGlobalSQL();
        this.provider = provider;
        setTabCompleter(new NavigationTabCompleter(globalSQL));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        NetworkUser user = instance.getUser(player);

        // If u is null, cancel.
        if (user == null) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        // Check if args is less than 1.
        if (args.length < 1) {
            // Send error message.
            error(user);
            return;
        }

        // Add
        switch (args[0].toUpperCase()) {

            // Add location
            case "ADD" -> addLocation(user);

            // Update location
            case "UPDATE" -> updateLocation(user, args);

            // Remove location
            case "REMOVE" -> removeLocation(user, args);

            // Suggested location
            case "SUGGESTED" -> suggestedLocation(user, args);

            // Subcategory subcommands
            case "SUBCATEGORY" -> subcategoryCommand(user, args);

            default -> error(user);
        }
    }

    private void addLocation(NetworkUser u) {
        if (u.hasPermission("uknet.navigation.request")) {
            if (u.mainGui != null) {
                u.mainGui.delete();
            }
            u.mainGui = new AddLocation(provider, AddLocationType.ADD);
            u.mainGui.open(u.player);
        } else {
            u.sendMessage(ChatUtils.error("You do not have permission to use this command."));
        }
    }

    private void updateLocation(NetworkUser u, String[] args) {
        if (!u.hasPermission("uknet.navigation.update")) {
            u.sendMessage(ChatUtils.error("You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            u.sendMessage(ChatUtils.error("/navigation update <location>"));
            return;
        }

        // Combine all args excluding the first, with spaces, since the name can be multiple words.
        String location = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Check if the location exists.
        if (!globalSQL
                .hasRow("SELECT location FROM location_data WHERE location=?;", location)) {
            u.sendMessage(ChatUtils.error("The location ")
                    .append(Component.text(location, NamedTextColor.DARK_RED))
                    .append(ChatUtils.error(" does not exist.")));
            return;
        }

        // Check if there is a marker on the map.
        if (globalSQL
                .hasRow("SELECT location FROM location_marker WHERE location=?;", location)) {
            u.sendMessage(ChatUtils.error("The location %s has a marker on the map, this must be removed first using " +
                            "%s",
                    location, String.format("/map remove %s", location)));
            return;
        }

        // Open update location menu.
        // They must be staff to access this.
        if (u.staffGui != null) {
            u.staffGui.delete();
        }

        // Get details from the location.
        Category category = Category.valueOf(globalSQL.getString("SELECT category FROM location_data WHERE location=?;", location));
        int subcategory_id = globalSQL.getInt("SELECT subcategory FROM location_data WHERE location=?;", location);
        String subcategory = null;
        if (subcategory_id != 0) {
            subcategory = globalSQL.getString("SELECT name FROM location_subcategory WHERE id=?;", subcategory_id);
        }
        int coordinate_id = globalSQL.getInt("SELECT coordinate FROM location_data WHERE location=?;", location);
        u.staffGui = new AddLocation(provider, AddLocationType.UPDATE, location, coordinate_id, category, subcategory);
        u.staffGui.open(u.player);
    }

    private void removeLocation(NetworkUser u, String[] args) {
        if (!u.hasPermission("uknet.navigation.remove")) {
            u.sendMessage(ChatUtils.error("You do not have permission to use this command."));
            return;
        }

        if (args.length < 2) {
            u.sendMessage(ChatUtils.error("/navigation remove <location>"));
            return;
        }

        // Combine all args excluding the first, with spaces, since the name can be multiple words.
        String location = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        // Check if the location exists.
        if (!globalSQL
                .hasRow("SELECT location FROM location_data WHERE location=?;", location)) {
            u.sendMessage(ChatUtils.error("The location ")
                    .append(Component.text(location, NamedTextColor.DARK_RED))
                    .append(ChatUtils.error(" does not exist.")));
            return;
        }

        // Check if there is a marker on the map.
        if (globalSQL
                .hasRow("SELECT location FROM location_marker WHERE location=?;", location)) {
            u.sendMessage(ChatUtils.error("The location %s has a marker on the map, this must be removed first using " +
                            "%s",
                    location, String.format("/map remove %s", location)));
            return;
        }

        // Delete location.
        globalSQL.update("DELETE FROM location_data WHERE location=?;", location);
        u.sendMessage(ChatUtils.success("Location ")
                .append(Component.text(location, NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" removed.")));
    }

    private void suggestedLocation(NetworkUser u, String[] args) {
        if (u.hasPermission("uknet.navigation.suggested")) {
            if (args.length > 1) {
                // Combine all args excluding the first, with spaces, since the name can be multiple words.
                String location = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                // Check if the location exists.
                if (globalSQL
                        .hasRow("SELECT location FROM location_data WHERE location=?;", location)) {
                    // Change suggested status of location.
                    if (globalSQL.hasRow("SELECT location FROM location_data WHERE location=? AND suggested=1;", location)) {
                        // Location is already suggested, remove that.
                        globalSQL.update("UPDATE location_data SET suggested=0 WHERE location=?;", location);
                        u.sendMessage(ChatUtils.success("The location ")
                                .append(Component.text(location, NamedTextColor.DARK_AQUA))
                                .append(ChatUtils.success(" will no longer be suggested.")));
                    } else {
                        // Set location as suggested.
                        globalSQL.update("UPDATE location_data SET suggested=1 WHERE location=?;", location);
                        u.sendMessage(ChatUtils.success("The location ")
                                .append(Component.text(location, NamedTextColor.DARK_AQUA))
                                .append(ChatUtils.success(" will now be suggested.")));
                    }
                } else {
                    u.sendMessage(ChatUtils.error("The location ")
                            .append(Component.text(location, NamedTextColor.DARK_RED))
                            .append(ChatUtils.error(" does not exist.")));
                }
            } else {
                u.sendMessage(ChatUtils.error("/navigation suggested <location>"));
            }
        } else {
            u.sendMessage(ChatUtils.error("You do not have permission to use this command."));
        }
    }

    private void subcategoryCommand(NetworkUser u, String[] args) {
        if (!u.hasPermission("uknet.navigation.subcategory")) {
            u.sendMessage(ChatUtils.error("You do not have permission to use this command."));
            return;
        }

        if (args.length < 3) {
            errorSubcategory(u);
            return;
        }

        switch (args[1].toUpperCase()) {

            // Add
            case "ADD" -> addSubcategory(u, args);

            // Remove
            case "REMOVE" -> removeSubcategory(u, args);

            default -> errorSubcategory(u);
        }
    }

    private void addSubcategory(NetworkUser u, String[] args) {
        if (args.length > 3) {
            // Check if arg[2] is a valid category.
            if (Arrays.stream(Category.values()).filter(Category::isSelectable)
                    .anyMatch(category -> category.toString().equalsIgnoreCase(args[2]))) {
                // Check that the subcategory does not yet exist.
                String name = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                if (globalSQL
                        .hasRow("SELECT name FROM location_subcategory WHERE name='" + name + "';")) {
                    u.sendMessage(ChatUtils.error("Subcategory ").append(Component.text(name,
                            NamedTextColor.DARK_RED)).append(ChatUtils.error(" already exists.")));
                } else {
                    globalSQL.update("INSERT INTO location_subcategory(name,category) " +
                            "VALUES('" + name + "','" + args[2].toUpperCase() + "');");
                    u.sendMessage(ChatUtils.success("Subcategory ").append(Component.text(name,
                                    NamedTextColor.DARK_AQUA))
                            .append(ChatUtils.success(" added to category "))
                            .append(Component.text(args[2].toUpperCase(), NamedTextColor.DARK_AQUA)));
                }
            } else {
                u.sendMessage(ChatUtils.error("Category ").append(Component.text(args[2], NamedTextColor.DARK_RED)
                        .append(ChatUtils.error(" is not valid."))));
            }
        } else {
            u.sendMessage(ERROR_SUBCATEGORY_ADD);
        }
    }

    private void removeSubcategory(NetworkUser u, String[] args) {
        if (args.length < 3) {
            u.sendMessage(ERROR_SUBCATEGORY_REMOVE);
        }

        // Check if a valid subcategory is listed.
        String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        int subcategory_id = globalSQL.getInt("SELECT id FROM location_subcategory WHERE " +
                "name='" + name + "';");

        if (subcategory_id == 0) {
            u.sendMessage(ChatUtils.error("Subcategory ").append(Component.text(name, NamedTextColor.DARK_RED))
                    .append(ChatUtils.error(" does not exist.")));
            return;
        }

        // Check if there is a marker on the map.
        if (globalSQL
                .hasRow("SELECT subcategory FROM location_marker WHERE subcategory=" + subcategory_id + ";")) {
            u.sendMessage(ChatUtils.error("The subcategory %s has a marker on the map, this must be removed first " +
                            "using %s",
                    name, String.format("/map remove %s", name)));
            return;
        }

        // Set all locations (and requests) with this subcategory to subcategory = null.
        globalSQL
                .update("UPDATE location_data SET subcategory=NULL WHERE subcategory=" + subcategory_id + ";");
        globalSQL.update("UPDATE location_requests SET subcategory=NULL WHERE " +
                "subcategory=" + subcategory_id + ";");
        // Remove the subcategory.
        globalSQL.update("DELETE FROM location_subcategory WHERE id=" + subcategory_id +
                ";");
        u.sendMessage(ChatUtils.error("Subcategory ").append(Component.text(name, NamedTextColor.DARK_RED))
                .append(ChatUtils.error(" removed.")));
    }

    private void error(NetworkUser u) {

        // If the player has permission for any of the commands send them the error.
        // Else tell them they don't have permission.
        if (u.hasAnyPermission("uknet.navigation.request", "uknet.navigation.update", "uknet.navigation.remove",
                "uknet.navigation.suggested", "uknet.navigation.subcategory")) {
            if (u.hasPermission("uknet.navigation.request")) {
                u.sendMessage(ChatUtils.error("/navigation add"));
            }

            if (u.hasPermission("uknet.navigation.update")) {
                u.sendMessage(ChatUtils.error("/navigation update <location>"));
            }

            if (u.hasPermission("uknet.navigation.remove")) {
                u.sendMessage(ChatUtils.error("/navigation remove <location>"));
            }

            if (u.hasPermission("uknet.navigation.suggested")) {
                u.sendMessage(ChatUtils.error("/navigation suggested <location>"));
            }

            if (u.hasPermission("uknet.navigation.subcategory")) {
                errorSubcategory(u);
            }
        } else {
            u.sendMessage(ChatUtils.error("You do not have permission to use this command."));
        }
    }

    private void errorSubcategory(NetworkUser u) {
        u.sendMessage(ERROR_SUBCATEGORY_ADD);
        u.sendMessage(ERROR_SUBCATEGORY_REMOVE);
    }

    @Override
    public String getLabel() {
        return "navigation";
    }

    @Override
    public String getDescription() {
        return "Adds commands to do with navigation.";
    }
}
