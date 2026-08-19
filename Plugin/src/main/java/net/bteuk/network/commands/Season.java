// package net.bteuk.network.commands;
//
// import io.papermc.paper.command.brigadier.CommandSourceStack;
// import net.bteuk.network.Network;
// import org.btuk.network.lib.utils.ChatUtils;
// import net.kyori.adventure.text.Component;
// import net.kyori.adventure.text.format.NamedTextColor;
// import org.bukkit.command.CommandSender;
// import org.bukkit.entity.Player;
// import org.jetbrains.annotations.NotNull;
//
// import java.util.Arrays;
//
// public class Season extends AbstractCommand {
//
//     private static final Component INVALID_COMMAND_ARGUMENTS = ChatUtils.error("/season create|start|end " +
//             "[season_name]");
//
//     @Override
//     public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
//
//         // Check if sender is player, then check permissions
//         CommandSender sender = stack.getSender();
//         if (sender instanceof Player) {
//             if (!hasPermission(sender, "uknet.season")) {
//                 return;
//             }
//         }
//
//         // Check args.
//         if (args.length >= 2) {
//
//             // Don't allow season command to alter the default season.
//             if (args[1].equalsIgnoreCase("default")) {
//                 sender.sendMessage(ChatUtils.error("The default season can not be modified!"));
//                 return;
//             }
//
//             // Check first arg.
//             if (args[0].equalsIgnoreCase("create")) {
//
//                 // Create season with all remaining args as name.
//                 sender.sendMessage(createSeason(args));
//                 return;
//             } else if (args[0].equalsIgnoreCase("start")) {
//
//                 // Start season with all remaining args as name.
//                 sender.sendMessage(startSeason(args));
//                 return;
//             } else if (args[0].equalsIgnoreCase("end")) {
//
//                 // End season with all remaining args as name.
//                 sender.sendMessage(endSeason(args));
//                 return;
//             }
//         }
//
//         // If the code reaches here then the command format was invalid.
//         sender.sendMessage(INVALID_COMMAND_ARGUMENTS);
//     }
//
//     private Component createSeason(String[] args) {
//
//         // Get the name from the remaining args.
//         String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
//
//         // Create the season if it does not already exist.
//         if (Network.getInstance().getGlobalSQL().hasRow("SELECT id FROM seasons WHERE id='" + name + "';")) {
//             return ChatUtils.error("A season with the name ")
//                     .append(Component.text(name, NamedTextColor.DARK_RED)
//                             .append(ChatUtils.error(" already exists.")));
//         }
//
//         if (Network.getInstance().getGlobalSQL().update("INSERT INTO seasons(id) VALUES('" + name + "');")) {
//             return ChatUtils.success("Season created with name ")
//                     .append(Component.text(name, NamedTextColor.DARK_AQUA));
//         } else {
//             return ChatUtils.success("An error occurred while creating the season, please contact a server admin.");
//         }
//     }
//
//     private Component startSeason(String[] args) {
//
//         // Get the name from the remaining args.
//         String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
//
//         // Check if the season exists, and is not already active.
//         if (!Network.getInstance().getGlobalSQL().hasRow("SELECT id FROM seasons WHERE id='" + name + "';")) {
//             return ChatUtils.error("A season with the name ")
//                     .append(Component.text(name, NamedTextColor.DARK_RED)
//                             .append(ChatUtils.error(" doesn't exists.")));
//         } else if (Network.getInstance().getGlobalSQL().hasRow("SELECT id FROM seasons WHERE id='" + name + "' AND " +
//                 "active=1;")) {
//             return ChatUtils.error("The season ")
//                     .append(Component.text(name, NamedTextColor.DARK_RED)
//                             .append(ChatUtils.error(" has already started.")));
//         } else if (Network.getInstance().getGlobalSQL().getInt("SELECT id FROM seasons WHERE id='" + name + "' AND " +
//                 "active=1;") > 1) {
//             return ChatUtils.error("There is already an active season, cancel season ")
//                     .append(Component.text(Network.getInstance().getGlobalSQL().getString("SELECT id FROM seasons " +
//                             "WHERE active=1 and id<>'default';"), NamedTextColor.DARK_RED))
//                     .append(ChatUtils.error(" first."));
//         }
//
//         if (Network.getInstance().getGlobalSQL().update("UPDATE seasons SET active=1 WHERE id='" + name + "';")) {
//             return ChatUtils.success("Started season ")
//                     .append(Component.text(name, NamedTextColor.DARK_AQUA));
//         } else {
//             return ChatUtils.success("An error occurred while starting the season, please contact a server admin.");
//         }
//     }
//
//     private Component endSeason(String[] args) {
//
//         // Get the name from the remaining args.
//         String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
//
//         // Check if the season exists, and is active.
//         if (!Network.getInstance().getGlobalSQL().hasRow("SELECT id FROM seasons WHERE id='" + name + "';")) {
//             return ChatUtils.error("A season with the name ")
//                     .append(Component.text(name, NamedTextColor.DARK_RED)
//                             .append(ChatUtils.error(" doesn't exists.")));
//         } else if (!Network.getInstance().getGlobalSQL().hasRow("SELECT id FROM seasons WHERE id='" + name + "' AND " +
//                 "active=1;")) {
//             return ChatUtils.error("The season ")
//                     .append(Component.text(name, NamedTextColor.DARK_RED)
//                             .append(ChatUtils.error(" is not active.")));
//         }
//
//         if (Network.getInstance().getGlobalSQL().update("UPDATE seasons SET active=0 WHERE id='" + name + "';")) {
//             return ChatUtils.success("Ended season ")
//                     .append(Component.text(name, NamedTextColor.DARK_AQUA));
//         } else {
//             return ChatUtils.success("An error occurred while starting the season, please contact a server admin.");
//         }
//     }
// }
