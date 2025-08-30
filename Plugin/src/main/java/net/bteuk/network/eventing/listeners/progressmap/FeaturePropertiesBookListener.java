// package net.bteuk.network.eventing.listeners.progressmap;
//
// import me.bteuk.progressmapper.guis.Field;
// import net.bteuk.network.Network;
// import net.bteuk.network.gui.progressmap.FeaturePageGUI;
// import net.bteuk.network.utils.NetworkUser;
// import net.kyori.adventure.text.TextComponent;
// import org.apache.commons.lang3.StringUtils;
// import org.bukkit.Bukkit;
// import org.bukkit.event.EventHandler;
// import org.bukkit.event.HandlerList;
// import org.bukkit.event.Listener;
// import org.bukkit.event.player.PlayerEditBookEvent;
// import org.bukkit.inventory.meta.BookMeta;
//
// public class FeaturePropertiesBookListener implements Listener {
//     private final NetworkUser user;
//     private final FeaturePageGUI featurePageGUI;
//     private final Field fieldType;
//
//     private String szRelevantTitle;
//
//     public FeaturePropertiesBookListener(FeaturePageGUI featurePageGUI, Field fieldType, NetworkUser user) {
//         this.featurePageGUI = featurePageGUI;
//         this.fieldType = fieldType;
//         this.user = user;
//
//         switch (fieldType) {
//             case Title:
//                 this.szRelevantTitle = ((BookMeta) (featurePageGUI.getFeatureMenu().getTitleBook().getItemMeta())).getTitle();
//                 break;
//             case Description:
//                 this.szRelevantTitle = ((BookMeta) (featurePageGUI.getFeatureMenu().getDescriptionBook().getItemMeta())).getTitle();
//                 break;
//             case Media_url:
//                 this.szRelevantTitle = ((BookMeta) (featurePageGUI.getFeatureMenu().getMedialURLBook().getItemMeta())).getTitle();
//                 break;
//         }
//     }
//
//     public void register(Network instance) {
//         Bukkit.getServer().getPluginManager().registerEvents(this, instance);
//     }
//
//     @EventHandler
//     public void BookCloseEvent(PlayerEditBookEvent e) {
//         // Check the player
//         if (!e.getPlayer().equals(user.player)) return;
//
//         // We can't actually also check the book because the event doesn't give that
//         // We can only check the title
//         if (!StringUtils.equalsIgnoreCase(e.getNewBookMeta().getTitle(), szRelevantTitle)) return;
//
//         // Extracts the new content from the book
//         String szNewContent = ((TextComponent) e.getNewBookMeta().page(1).asComponent()).content();
//
//         // Edits the Feature's details in the feature menu
//         featurePageGUI.getFeatureMenu().fieldEdit(fieldType, szNewContent);
//
//         // Reopen the feature menu
//         featurePageGUI.refresh(); // Refresh will redo the GUI item texts based on the values edited in the line above
//         user.mainGui = featurePageGUI;
//         user.mainGui.open(user.player);
//
//         // Unregisters this listener
//         unregister(); // Do I actually want this? Surely I want the book to be re-editable - No. No need really,
//         // makes it more confusing for the player
//         // If they just click the item to get a single use book and then the book closes, then that's a lot simpler
//
//         // Removes the book
//         user.player.getInventory().getItemInMainHand().setAmount(0);
//     }
//
//     private void unregister() {
//         HandlerList.unregisterAll(this);
//     }
// }
