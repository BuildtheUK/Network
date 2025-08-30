// package net.bteuk.network.commands.navigation;
//
// import io.papermc.paper.command.brigadier.CommandSourceStack;
// import net.bteuk.network.commands.AbstractCommand;
// import net.bteuk.network.utils.SwitchServer;
// import org.bukkit.entity.Player;
// import org.jetbrains.annotations.NotNull;
//
// public class BTEUK extends AbstractCommand {
//
//     @Override
//     public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
//
//         // Check if the sender is a player.
//         Player player = getPlayer(stack);
//         if (player == null) {
//             return;
//         }
//
//         SwitchServer.switchToExternalServer(player);
//     }
// }
