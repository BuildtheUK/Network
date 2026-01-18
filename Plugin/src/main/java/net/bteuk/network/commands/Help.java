package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.api.entity.Role;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Help extends AbstractCommand {

    private static final Component ROLE_ERROR = ChatUtils.error("An error occurred while loading a role, please " +
            "contact an administrator.");

    private final Constants constants;
    private final Roles roles;

    public Help(Constants constants, Roles roles) {
        this.constants = constants;
        this.roles = roles;
        setTabCompleter(new FixedArgSelector(Arrays.asList("building", "explore", "plots", "regions", "utils",
                "worldedit"), 0));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // Default message.
        if (args.length == 0) {
            help(player);
            return;
        }

        // Check for the first argument to determine the message.
        switch (args[0]) {

            // Building
            case "building" -> building(player);

            // Exploring
            case "explore" -> explore(player);

            // Plots
            case "plots" -> plots(player);

            // Regions
            case "regions" -> regions(player);

            // Utilities
            case "utils" -> utils(player);

            // Worldedit
            case "worldedit" -> worldedit(player);

            // Default for any other arguments.
            default -> help(player);
        }
    }

    private void help(Player p) {

        p.sendMessage(Utils.title("Help:"));

        // Navigator
        p.sendMessage(Component.text("/navigator", NamedTextColor.GRAY).append(Utils.line(" - Click to open the " +
                        "navigator, access most server features from here."))
                .clickEvent(ClickEvent.runCommand("/navigator")));

        // Exporing
        p.sendMessage(Component.text("/help explore", NamedTextColor.GRAY).append(Utils.line(" - Click to list " +
                        "commands used for exploring the server."))
                .clickEvent(ClickEvent.runCommand("/help explore")));

        // Building
        p.sendMessage(Component.text("/help building", NamedTextColor.GRAY).append(Utils.line(" - Click for " +
                        "information on how to rank-up and build."))
                .clickEvent(ClickEvent.runCommand("/help building")));

        // Plots
        // Information about plots.
        if (constants.plotSystemEnabled()) {
            p.sendMessage(Component.text("/help plots", NamedTextColor.GRAY).append(Utils.line(" - Click for details on " +
                            "plots and how to use them."))
                    .clickEvent(ClickEvent.runCommand("/help plots")));
        }

        // Regions
        // Information about regions.
        if (constants.regionsEnabled()) {
            p.sendMessage(Component.text("/help regions", NamedTextColor.GRAY).append(Utils.line(" - Click for " +
                            "information about regions and why we have them."))
                    .clickEvent(ClickEvent.runCommand("/help regions")));
        }

        // Utilities
        p.sendMessage(Component.text("/help utils", NamedTextColor.GRAY).append(Utils.line(" - Click for other " +
                        "commands that can be useful in general."))
                .clickEvent(ClickEvent.runCommand("/help utils")));

        // Worldedit
        p.sendMessage(Component.text("/help worldedit", NamedTextColor.GRAY).append(Utils.line(" - Click to list " +
                        "available WorldEdit commands."))
                .clickEvent(ClickEvent.runCommand("/help worldedit")));
    }

    private void building(Player p) {

        // Building is partially role-specific.
        // Get the current builder-role of the player.
        Role builderRole = roles.builderRole(p);
        if (builderRole == null) {
            p.sendMessage(ChatUtils.error("You have an invalid role, please contact an administrator."));
            return;
        }

        p.sendMessage(Utils.title("Building on the server:"));

        Component roleMessage = Utils.line("You currently have the builder role ")
                .append(builderRole.getColouredRoleName());

        Component roleSpecificMessage = switch (builderRole.getId()) {
            case "reviewer" -> {
                Role architect = roles.getRoleById("architect");
                if (architect == null) {
                    yield null;
                }
                yield ChatUtils.line("A ")
                        .append(builderRole.getColouredRoleName())
                        .append(ChatUtils.line(" has all the functions of an "))
                        .append(architect.getColouredRoleName())
                        .append(ChatUtils.line(" but can also review plots, region requests and navigation requests."));
            }

            case "architect" -> {
                Role builder = roles.getRoleById("builder");
                if (builder == null) {
                    yield null;
                }
                yield ChatUtils.line("An ")
                        .append(builderRole.getColouredRoleName())
                        .append(ChatUtils.line(" has all the functions of a "))
                        .append(builder.getColouredRoleName())
                        .append(ChatUtils.line(" and can create zones in the plotsystem and create new plots."));
            }

            case "builder" -> ChatUtils.line("A ")
                    .append(builderRole.getColouredRoleName())
                    .append(ChatUtils.line(" can claim regions without needing staff approval as well as claim plots " +
                            "of all difficulties in the plot system. " /*+
                "To be promoted to &7Architect &fyou need x building points in the last 30 days."*/));

            case "jrbuilder" -> {
                Role builder = roles.getRoleById("builder");
                if (builder == null) {
                    yield null;
                }
                yield builderRole.getColouredRoleName()
                        .append(ChatUtils.line(" does not require approval for all claims, " +
                                "however in busy areas reviewers will need to review the request. " +
                                "Complete 1 hard plot to be promoted to "))
                        .append(builder.getColouredRoleName());
            }

            case "apprentice" -> {
                Role jrbuilder = roles.getRoleById("jrbuilder");
                if (jrbuilder == null) {
                    yield null;
                }
                yield builderRole.getColouredRoleName()
                        .append(ChatUtils.line(" is the first role which allows you to claim regions across the country, " +
                                "however, all region requests must be approved by staff."))
                        .append(ChatUtils.line("Complete 1 normal plot to be promoted to "))
                        .append(jrbuilder.getColouredRoleName());
            }

            case "applicant" -> {
                Role apprentice = roles.getRoleById("apprentice");
                if (apprentice == null) {
                    yield null;
                }
                yield builderRole.getColouredRoleName()
                        .append(ChatUtils.line(" is achieved by completing the required steps in the tutorial, you " +
                                "now have access to the plot system. " +
                                "Complete 1 easy plot to be promoted to "))
                        .append(apprentice.getColouredRoleName());
            }

            case "default" -> ChatUtils.line("The role you start with when you first join the server. To start your path " +
                    "towards becoming a builder just hop into the tutorial.");

            default -> null;
        };

        if (roleSpecificMessage == null) {
            p.sendMessage(ROLE_ERROR);
            return;
        }

        p.sendMessage(roleMessage);
        p.sendMessage(roleSpecificMessage);

        // Tpll and ll.
        if (constants.tpllEnabled()) {
            p.sendMessage(Component.text("/tpll <lat> <lon> [altitude]", NamedTextColor.GRAY)
                    .append(Utils.line(" - Teleport to the coordinates provided, altitude is optional.")));
        }
        if (constants.ll()) {
            p.sendMessage(Component.text("/ll", NamedTextColor.GRAY)
                    .append(Utils.line(" - Get the real life coordinates of your current location, with a link to Google " +
                            "Maps.")));
        }

        if (constants.skullsEnabled()) {
            p.sendMessage(Component.text("/skulls", NamedTextColor.GRAY)
                    .append(Utils.line(" - Opens the head menu. To search for a specific head use "))
                    .append(Component.text("/skulls search")));
        }
        p.sendMessage(Component.text("/bannermaker", NamedTextColor.GRAY)
                .append(Utils.line(" - Opens the bannermaker menu, allows you to create and save banners easily.")));
    }

    private void explore(Player p) {

        Role builderRole = roles.builderRole(p);
        if (builderRole == null) {
            p.sendMessage(ChatUtils.error("You have an invalid role, please contact an administrator."));
            return;
        }

        p.sendMessage(Utils.title("Exploring the server:"));

        // Exploring using the gui or map.
        if (constants.warpsEnabled()) {
            p.sendMessage(Utils.line("Using the navigator ")
                    .append(Component.text("/navigator", NamedTextColor.GRAY))
                    .append(Utils.line(" you can access many locations that are being or have been built on the server.")));
        }
        if (!constants.standalone()) {
            p.sendMessage(Utils.line("Alternatively you can use the ")
                    .append(Component.text("/map", NamedTextColor.GRAY))
                    .append(Utils.line(" located in the "))
                    .append(Component.text("/lobby", NamedTextColor.GRAY)));
        }

        // Tpll can be used otherwise but for roles without region access they can't load new terrain.
        // For Jr.Builder also explain how to request new locations.

        switch (builderRole.getId()) {

            case "applicant", "default" -> {
                if (constants.tpllEnabled()) {
                    p.sendMessage(Utils.line("\nTo access other areas you can try using ")
                            .append(Component.text("/tpll <lat> <lon>", NamedTextColor.GRAY))
                            .append(Utils.line(", however you will only be able to teleport to locations that have " +
                                    "already been generated on the server.")));
                }
            }

            case "apprentice" -> {
                if (constants.tpllEnabled()) {
                    p.sendMessage(Utils.line("\nTo access other areas you can try using ")
                            .append(Component.text("/tpll <lat> <lon>", NamedTextColor.GRAY)));
                }
            }

            default -> {
                if (constants.warpsEnabled()) {
                    p.sendMessage(Utils.line("\nYou can request new locations to be added to the navigator, " +
                            "this can be done by standing at the location you want to add and then clicking on 'Add " +
                            "Location' in the exporation menu. " +
                            "Please only request locations with a decent bit of progress."));
                }

                if (constants.tpllEnabled()) {
                    p.sendMessage(Utils.line("\nTo access other areas you can try using ")
                            .append(Component.text("/tpll <lat> <lon>", NamedTextColor.GRAY))
                            .append(Utils.line(", however please don't generate new terrain for no good reason.")));
                }
            }
        }

        // Home command for saving personal locations.
        if (constants.homesEnabled()) {
            switch (builderRole.getId()) {
                case "apprentice", "applicant", "default" -> p.sendMessage(Utils.line("\nYou can set a ")
                        .append(Component.text("/home", NamedTextColor.GRAY))
                        .append(Utils.line(" using "))
                        .append(Component.text("/sethome", NamedTextColor.GRAY)));

                default -> p.sendMessage(Utils.line("\nYou can set ")
                        .append(Component.text("/homes", NamedTextColor.GRAY))
                        .append(Utils.line(" using "))
                        .append(Component.text("/sethome <name>", NamedTextColor.GRAY)));
            }
        }
    }

    private void plots(Player p) {

        // Get the roles needed for the text.
        Role applicant = roles.getRoleById("applicant");
        Role apprentice = roles.getRoleById("apprentice");
        Role jrbuilder = roles.getRoleById("jrbuilder");
        Role builder = roles.getRoleById("builder");

        if (applicant == null || apprentice == null || jrbuilder == null || builder == null) {
            p.sendMessage(ROLE_ERROR);
            return;
        }

        p.sendMessage(Utils.title("Plots:"));

        p.sendMessage(Utils.line("A plot will usually include a building or row of connected buildings. " +
                "The goal of a plot is to complete that building and then submit it."));

        p.sendMessage(Utils.line("\nYou can claim a plot in the building menu or using ")
                .append(Component.text("/claim", NamedTextColor.GRAY))
                .append(Utils.line(" while standing in a plot.")));

        p.sendMessage(Utils.line("\nPlots can be managed in the plot menu which is accessed from the build menu, from" +
                " here you can also invite others to your plot."));

        p.sendMessage(Utils.line("\nBy completing plots of various difficulties you are able to rank up."));
        p.sendMessage(
                applicant.getColouredRoleName()
                        .append(Utils.line(" -> "))
                        .append(apprentice.getColouredRoleName())
                        .append(Utils.line(" by completing an easy plot.")));

        p.sendMessage(
                apprentice.getColouredRoleName()
                        .append(Utils.line(" -> "))
                        .append(jrbuilder.getColouredRoleName())
                        .append(Utils.line(" by completing a normal plot.")));

        p.sendMessage(
                jrbuilder.getColouredRoleName()
                        .append(Utils.line(" -> "))
                        .append(builder.getColouredRoleName())
                        .append(Utils.line(" by completing a hard plot.")));

        // p.sendMessage(Utils.chat("&7\nArchitects &fare able to create new plots, more info on this using &7/help
        // architect&f."));

    }

    private void regions(Player p) {

        p.sendMessage(Utils.title("Regions:"));

        p.sendMessage(Utils.line("A region represents a 512 by 512 area in a grid of regions, they cover the whole " +
                "world."));
        p.sendMessage(Utils.line("Regions provide an added layer of security and accountability in determining who " +
                "has built where and when. " +
                "We keep track of every player who joins a region, this allows us to trace back any wrongdoers with " +
                "relative ease."));

        Role apprentice = roles.getRoleById("apprentice");
        p.sendMessage(Utils.line("\nTo join a region you must be a ")
                .append(apprentice.getColouredRoleName())
                .append(Utils.line(" or above, this is both to prevent griefers and to ensure building standards.")));

        p.sendMessage(Utils.line("\nYou can join a region by clicking on the dark oak door in the build menu. " +
                "If the region already has an owner then they must accept your request before you are able to build " +
                "in the region."));
        p.sendMessage(Utils.line("Region owners can also invite you to their region using the gui, you will then be " +
                "notified in chat."));
    }

    private void utils(Player p) {

        p.sendMessage(Utils.title("Utilities:"));

        p.sendMessage(Component.text("/tp <player>", NamedTextColor.GRAY)
                .append(Utils.line(" - Teleport to a specific player anywhere on the server.")));

        p.sendMessage(Component.text("/back", NamedTextColor.GRAY)
                .append(Utils.line(" - Return to the location prior to your last teleport.")));

        p.sendMessage(Component.text("\n/speed [0-10]", NamedTextColor.GRAY)
                .append(Utils.line(" - Set your flying/walking speed.")));

        p.sendMessage(Component.text("/nv", NamedTextColor.GRAY)
                .append(Utils.line(" - Toggle night vision, also removes glitched shadows.")));

        if (!constants.standalone()) {
            p.sendMessage(Component.text("\n/discord", NamedTextColor.GRAY)
                    .append(Utils.line(" - Sends you a link to our Discord server.")));
        }
    }

    private void worldedit(Player p) {

        p.sendMessage(Utils.title("WorldEdit:"));

        p.sendMessage(Component.text("//wand", NamedTextColor.GRAY)
                .append(Utils.line(" - Gives you the selection tool for WorldEdit.")));
        p.sendMessage(Utils.line("Left click to select your first point, right click to add a second (or more for " +
                "certain selection types)."));

        p.sendMessage(Component.text("\n//set <block>", NamedTextColor.GRAY)
                .append(Utils.line(" - Sets the area you've selected to the specified block.")));

        p.sendMessage(Component.text("//replace <block> <block>", NamedTextColor.GRAY)
                .append(Utils.line(" - Replaces the specified block with another block in your selection.")));

        p.sendMessage(Component.text("//line <block>", NamedTextColor.GRAY)
                .append(Utils.line(" - Creates a line between your two selected points.")));

        p.sendMessage(Component.text("\n//undo", NamedTextColor.GRAY)
                .append(Utils.line(" and "))
                .append(Component.text("//redo", NamedTextColor.GRAY))
                .append(Utils.line(" allow you to undo or redo any WorldEdit command.")));

        Component worldEditMessage = Utils.line("For more information you can reference: ")
                .append(Component.text("https://worldedit.enginehub.org/en/latest/usage/", NamedTextColor.GRAY));
        worldEditMessage = worldEditMessage.clickEvent(ClickEvent.openUrl("https://worldedit.enginehub.org/en/latest/usage/"));
        p.sendMessage(worldEditMessage);
    }

    @Override
    public String getLabel() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Help menu for information on commands and server features.";
    }
}
