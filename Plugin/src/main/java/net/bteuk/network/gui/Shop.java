// package net.bteuk.network.gui;
//
// import net.bteuk.network.Network;
// import net.bteuk.network.utils.Utils;
// import net.kyori.adventure.text.Component;
// import net.kyori.adventure.text.format.NamedTextColor;
// import net.kyori.adventure.text.format.TextDecoration;
// import org.bukkit.Material;
//
// public class Shop extends Gui {
//
//     public Shop() {
//
//         super(27, Component.text("Shop", NamedTextColor.AQUA, TextDecoration.BOLD));
//
//         createGui();
//     }
//
//     private void createGui() {
//
//         setItem(10, Utils.createItem(Material.PINK_CONCRETE_POWDER, 1,
//                         Utils.title("2023 Card Pack"),
//                         Utils.line("Price: 1000")),
//                 (NetworkUser u) -> {
//                 }
//         );
//
//         // Return
//         setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
//                         Utils.title("Return"),
//                         Utils.line("Open the navigator main menu.")),
//                 u ->
//
//                 {
//
//                     // Delete this gui.
//                     this.delete();
//                     u.mainGui = null;
//
//                     // Switch to navigation menu.
//                     Network.getInstance().navigatorGui.open(u.player);
//                 });
//     }
//
//     public void refresh() {
//
//         this.clearGui();
//         createGui();
//     }
// }
