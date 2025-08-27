package net.bteuk.network.eventing.listeners.progressmap;

import me.bteuk.progressmapper.GeometryEditor;
import net.bteuk.network.Network;
import net.bteuk.network.gui.progressmap.FeaturePageGUI;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class FeatureGeometryEditorListener extends Gui implements Listener {
    private final Network plugin;
    private final FeaturePageGUI featurePageGUI;
    private final NetworkUser user;
    private final ItemStack blazeRod;
    // Used for controlling editions to the geometry of the feature, also has access to the feature itself
    private final GeometryEditor geometryEditor;
    private int iTaskID;

    public FeatureGeometryEditorListener(Network plugin, FeaturePageGUI featurePageGUI, GeometryEditor geometryEditor
            , NetworkUser user, ItemStack blazeRod) {
        super(geometryEditor.getGUI());
        this.plugin = plugin;
        this.featurePageGUI = featurePageGUI;
        this.geometryEditor = geometryEditor;
        this.user = user;
        this.blazeRod = blazeRod;
    }

    private void setActions() {
        setAction(2, u ->
        {
            selectionCancel();
        });

        setAction(6, u ->
        {
            selectionConfirm();
        });
    }

    public void register() {
        // Registers the selection listener
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        // Sets up the outline view entity spawn schedule
        this.iTaskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () ->
        {
            geometryEditor.updateView();
        }, 20L, 20L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void BlockHitWithBlazeRodEvent(PlayerInteractEvent e) {
        if (!e.hasBlock())
            return;
        if (!e.getPlayer().getUniqueId().equals(this.user.player.getUniqueId()))
            return;
        if (!e.getMaterial().equals(Material.BLAZE_ROD))
            return;

        e.setCancelled(true);

        // Stores the clicked block in a local variable
        Block clickedBlock = e.getClickedBlock();

        // Now we can determine the plugin action
        if (e.getAction().isLeftClick()) {
            geometryEditor.leftClick(clickedBlock.getX(), clickedBlock.getZ());
            user.player.sendMessage(Component.text("Area restarted", NamedTextColor.AQUA));
        } else if (e.getAction().isRightClick()) {
            geometryEditor.rightClick(clickedBlock.getX(), clickedBlock.getZ());
            user.player.sendMessage(Component.text("Corner added to shape", NamedTextColor.AQUA));
        }
    }

    public void selectionConfirm() {
        // Saves the Feature's geometry
        geometryEditor.confirmGeometry();

        // Deletes this gui
        this.delete();

        // Reopen the feature menu
        featurePageGUI.refresh(); // Refresh will redo the GUI item texts
        user.mainGui = featurePageGUI;
        user.mainGui.open(user);

        // Unregisters the selection listener and cancels the outline view
        unregister();
    }

    public void selectionCancel() {
        // Deletes this gui
        this.delete();

        // Reset the mc blocks of the area selection to be that of the current area on the progress map (or more
        // accurately as saved locally in the feature object)
        geometryEditor.convertFeatureGeometryIntoBlockCoordinates();

        // Reopen the feature menu
        featurePageGUI.refresh(); // Refresh will redo the GUI item texts
        user.mainGui = featurePageGUI;
        user.mainGui.open(user);

        // Unregisters the selection listener and cancels the outline view
        unregister();
    }

    private void unregister() {
        // Unregisters the selection listener
        HandlerList.unregisterAll(this);

        // Cancels the outline view
        Bukkit.getScheduler().cancelTask(iTaskID);

        // Removes the blaze rod
        blazeRod.setAmount(0);
    }

    @Override
    public void refresh() {
        // Refresh icons
        this.clearGui();
        Inventory inventory = geometryEditor.getGUI();
        this.getInventory().setContents(inventory.getContents());

        // Refresh actions
        setActions();
    }
}
