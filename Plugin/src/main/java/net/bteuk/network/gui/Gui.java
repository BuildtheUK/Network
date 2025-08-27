package net.bteuk.network.gui;

import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public abstract class Gui implements GuiInterface {

    public static final Map<UUID, Gui> inventoriesByUUID = new HashMap<>();
    public static final Map<UUID, UUID> openInventories = new HashMap<>();

    // Information about the gui.
    private final UUID uuid;
    private final Inventory inv;
    private final Map<Integer, guiAction> actions;

    @Setter
    private boolean deleteOnClose = false;

    public Gui(int invSize, Component invName) {

        uuid = UUID.randomUUID();
        inv = Bukkit.createInventory(null, invSize, invName);
        actions = new HashMap<>();
        inventoriesByUUID.put(getUuid(), this);
    }

    public Gui(Inventory inv) {
        this.inv = inv;
        uuid = UUID.randomUUID();
        actions = new HashMap<>();
        inventoriesByUUID.put(getUuid(), this);
    }

    public Inventory getInventory() {
        return inv;
    }

    public void setItem(int slot, ItemStack stack, guiAction action) {

        inv.setItem(slot, stack);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    public void setItem(int slot, ItemStack stack) {
        setItem(slot, stack, null);
    }

    public void setAction(int slot, guiAction action) {

        if (action != null) {
            actions.put(slot, action);
        }
    }

    public void clearGui() {
        inv.clear();
        actions.clear();
    }

    public void open(NetworkUser u) {
        u.player.openInventory(inv);
        openInventories.put(u.player.getUniqueId(), getUuid());
    }

    public void delete() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            openInventories.remove(p.getUniqueId(), getUuid());
        }
        inventoriesByUUID.remove(getUuid());
    }

    public interface guiAction {
        void click(NetworkUser u);
    }
}
