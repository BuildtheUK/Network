// package net.bteuk.network.commands.staff;
//
// import io.papermc.paper.command.brigadier.CommandSourceStack;
// import net.bteuk.network.Network;
// import net.bteuk.network.commands.AbstractCommand;
// import net.bteuk.network.lib.utils.ChatUtils;
// import net.bteuk.network.utils.progression.Progression;
// import org.bukkit.command.CommandSender;
// import org.bukkit.entity.Player;
// import org.jetbrains.annotations.NotNull;
//
// public class Exp extends AbstractCommand {
//
//     @Override
//     public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
//
//         // Check if sender is player, then check permissions
//         CommandSender sender = stack.getSender();
//         if (sender instanceof Player) {
//             if (!hasPermission(sender, "uknet.exp")) {
//                 return;
//             }
//         }
//
//         if (args.length < 3) {
//             return;
//         }
//
//         if (args[0].equals("give")) {
//             String uuid =
//                     Network.getInstance().getGlobalSQL()
//                             .getString("SELECT uuid FROM player_data WHERE name='" + args[1] + "';");
//             if (uuid == null) {
//                 sender.sendMessage(ChatUtils.error("Player " + args[1] + " could not be found."));
//             } else {
//                 try {
//                     int val = Integer.parseInt(args[2]);
//                     Progression.addExp(uuid, val);
//                 } catch (NumberFormatException ignored) {
//                 }
//             }
//         }
//     }
// }
