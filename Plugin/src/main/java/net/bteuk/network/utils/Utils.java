package net.bteuk.network.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.bteuk.network.Network;
import net.bteuk.network.core.Constants;
import net.bteuk.network.lib.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final Pattern PATTERN = Pattern.compile("%s");

    public static Component title(String message) {
        return Component.text(message, NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
    }

    public static Component line(String message) {
        return colouredText(NamedTextColor.WHITE, message);
    }

    public static Component greyText(String message) {
        return colouredText(NamedTextColor.GRAY, message);
    }

    public static Component error(String message) {
        return Component.text(message, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Create an error message with vars.
     * The colour of the message is RED, with the vars highlighted with DARK_RED.
     *
     * @param message the message, using %s as placeholder for the vars.
     * @param vars    the vars to add to the placeholders, must equal the number of placeholder symbols.
     * @return the {@link Component} with the message, or null if the number of vars is incorrect.
     */
    public static Component error(String message, String... vars) {
        return varMessage(NamedTextColor.RED, NamedTextColor.DARK_RED, message, vars);
    }

    public static Component success(String message) {
        return colouredText(NamedTextColor.GREEN, message);
    }

    /**
     * Create a success message with vars.
     * The colour of the message is GREEN, with the vars highlighted with DARK_AQUA.
     *
     * @param message the message, using %s as placeholder for the vars.
     * @param vars    the vars to add to the placeholders, must equal the number of placeholder symbols.
     * @return the {@link Component} with the message, or null if the number of vars is incorrect.
     */
    public static Component success(String message, String... vars) {
        return varMessage(NamedTextColor.GREEN, NamedTextColor.DARK_AQUA, message, vars);
    }

    private static Component varMessage(NamedTextColor textColour, NamedTextColor varColour, String message, String... vars) {
        Component component = Component.empty();
        // Find the number of vars needed.
        int lastIdx = 0;
        int count = 0;
        Matcher matcher = PATTERN.matcher(message);
        while (matcher.find()) {
            int idx = matcher.start();
            if (idx != lastIdx) {
                component = component.append(colouredText(textColour, message.substring(lastIdx, idx)));
            }
            if (count < vars.length) {
                component = component.append(colouredText(varColour, vars[count]));
                count++;
            }
            lastIdx = idx + 2;
        }
        // At the remaining text if exists.
        if (lastIdx < message.length()) {
            component = component.append(colouredText(textColour, message.substring(lastIdx)));
        }
        return component;
    }

    private static Component colouredText(NamedTextColor colour, String message) {
        return Component.text(message, colour).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, false);
    }

    /**
     * Adds the prefix to a tip message.
     *
     * @param tip the tip to add a prefix to
     * @return Component of the tip with the prefix
     */
    public static Component tip(Component tip) {
        return Component.text("[TIP] ", TextColor.color(0x346beb))
                .append(tip.colorIfAbsent(TextColor.color(0xffffff)));
    }

    public static ItemStack createItem(Material material, int amount, Component displayName, Component... loreString) {

        ItemStack item = ItemStack.of(material.isItem() ? material : Material.STRUCTURE_VOID);
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createCustomSkullWithFallback(Network instance, String texture, Material fallback, int amount, Component displayName, Component... loreString) {

        ItemStack item;

        try {

            if (texture == null) {
                throw new NullPointerException();
            }

            URL url = new URL("http://textures.minecraft.net/texture/" + texture);
            item = ItemStack.of(Material.PLAYER_HEAD);

            SkullMeta meta = (SkullMeta) item.getItemMeta();

            // Create playerprofile.
            PlayerProfile profile = instance.getServer().createProfile(UUID.randomUUID());

            PlayerTextures textures = profile.getTextures();
            textures.setSkin(url);

            profile.setTextures(textures);

            meta.setPlayerProfile(profile);

            item.setItemMeta(meta);
        } catch (Exception e) {
            item = ItemStack.of(fallback);
        }

        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createPlayerSkull(String uuid, int amount, Component displayName, Component... loreString) {

        ItemStack item;

        item = ItemStack.of(Material.PLAYER_HEAD);
        item.setAmount(amount);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);

        OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        meta.setOwningPlayer(p);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createPlayerSkull(PlayerProfile profile, int amount, Component displayName, Component... loreString) {

        ItemStack item;

        item = ItemStack.of(Material.PLAYER_HEAD);
        item.setAmount(amount);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);

        return item;
    }

    public static ItemStack createPotion(Material material, PotionEffectType effect, int amount, Component displayName, Component... loreString) {

        ItemStack item;

        item = ItemStack.of(material);
        item.setAmount(amount);

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.addCustomEffect(new PotionEffect(effect, Integer.MAX_VALUE, 1), true);

        meta.displayName(displayName);
        List<Component> lore = new ArrayList<>(Arrays.asList(loreString));
        meta.lore(lore);
        item.setItemMeta(meta);

        return item;
    }

    public static int getHighestYAt(Constants constants, World w, int x, int z) {

        for (int i = (constants.maxY() - 1); i >= constants.minY(); i--) {
            if (w.getBlockAt(x, i, z).getType() != Material.AIR) {
                return i + 1;
            }
        }
        // Return 65 as the default y.
        return 65;
    }

    /**
     * Gives a player an item, it will be set in their main hand, if it does not already exist there.
     * <p>
     * If the main hand is empty, set it there.
     * If then main hand is slot 8 and includes the navigator, find the first empty slot available and set it there.
     * If no empty slots are available set it to slot 7.
     * If the main hand has an item swap the current item to an empty slot in the inventory.
     * If no empty slots are available overwrite it.
     */
    public static void giveItem(Network instance, Player p, ItemStack item, String name) {

        ItemStack currentItem = p.getInventory().getItemInMainHand();

        int emptySlot = getEmptyHotbarSlot(p);

        boolean hasNavigator = (p.getInventory().getHeldItemSlot() == 8 && currentItem.equals(instance.getNavigatorItem()));
        boolean hasItemAlready = p.getInventory().containsAtLeast(item, 1);

        // If we already have the item switch to current slot.
        if (hasItemAlready) {

            // Switch item to current slot.
            int slot = p.getInventory().first(item);

            if (hasNavigator) {

                p.getInventory().setItem(slot, p.getInventory().getItem(7));
                p.getInventory().setItem(7, item);
                p.sendMessage(ChatUtils.success("Set ").append(Component.text(name, NamedTextColor.DARK_AQUA).append(ChatUtils.success(" to slot 8"))));
            } else {

                p.getInventory().setItem(slot, p.getInventory().getItemInMainHand());
                p.getInventory().setItemInMainHand(item);
                p.sendMessage(ChatUtils.success("Set ").append(Component.text(name, NamedTextColor.DARK_AQUA).append(ChatUtils.success(" to main hand."))));
            }
        } else if (emptySlot >= 0) {
            // The current slot is empty. This also implies no navigator, and thus the item does not yet exist in the
            // inventory.
            // Set item to empty slot.
            p.getInventory().setItem(emptySlot, item);
            p.sendMessage(ChatUtils.success("Set ").append(Component.text(name, NamedTextColor.DARK_AQUA).append(ChatUtils.success(" to slot " + (emptySlot + 1)))));
        } else if (hasNavigator) {
            // Player has no empty slots and is holding the navigator, set to item to slot 7.
            p.getInventory().setItem(7, item);
            p.sendMessage(ChatUtils.success("Set ").append(Component.text(name, NamedTextColor.DARK_AQUA).append(ChatUtils.success(" to slot 8"))));
        } else {
            // Set item in current slot.
            p.getInventory().setItemInMainHand(item);
            p.sendMessage(ChatUtils.success("Set ").append(Component.text(name, NamedTextColor.DARK_AQUA).append(ChatUtils.success(" to main hand."))));
        }
    }

    // Return an empty hotbar slot, if no empty slot exists return -1.
    public static int getEmptyHotbarSlot(Player p) {

        // If main hand is empty return that slot.
        ItemStack heldItem = p.getInventory().getItemInMainHand();
        if (heldItem.getType() == Material.AIR) {
            return p.getInventory().getHeldItemSlot();
        }

        // Check if hotbar has an empty slot.
        for (int i = 0; i < 9; i++) {

            ItemStack item = p.getInventory().getItem(i);

            if (item == null) {
                return i;
            }
            if (item.getType() == Material.AIR) {
                return i;
            }
        }

        // No slot could be found, return -1.
        return -1;
    }

    public static void enchant(ItemStack itemStack) {
        itemStack.addUnsafeEnchantment(Enchantment.MENDING, 1);
        // itemStack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }
}
