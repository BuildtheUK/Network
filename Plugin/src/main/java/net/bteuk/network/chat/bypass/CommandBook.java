package net.bteuk.network.chat.bypass;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.btuk.minecraft.component.ComponentUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@Log
public final class CommandBook {

    public static final Component TITLE = ComponentUtils.title("Command Book");
    private static final String KEY = "command_book";

    private CommandBook() {
    }

    private static NamespacedKey commandBookKey(JavaPlugin plugin) {
        return new NamespacedKey(plugin, KEY);
    }

    public static boolean isCommandBook(JavaPlugin plugin, ItemStack item) {
        if (item == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(commandBookKey(plugin), PersistentDataType.BYTE);
    }

    public static void ensureUserHasSingleCommandBook(Network plugin, NetworkUser user) {
        ItemStack commandBook = null;
        List<ItemStack> itemsToRemove = new ArrayList<>();
        for (ItemStack item : user.player.getInventory().getContents()) {
            if (commandBook == null && isCommandBook(plugin, item)) {
                commandBook = item;
            } else if (isCommandBook(plugin, item)) {
                log.info("Player has multiple command books, removing duplicates.");
                itemsToRemove.add(item);
            }
        }
        for (ItemStack item : itemsToRemove) {
            user.player.getInventory().remove(item);
        }
        if (commandBook == null) {
            commandBook = createCommandBook(plugin);

            // If not, give it to them in slot 8 (index 7) if it's empty, otherwise find the first empty slot.
            ItemStack slot8Item = user.player.getInventory().getItem(7);
            if (slot8Item == null) {
                user.player.getInventory().setItem(7, commandBook);
            } else {
                user.player.getInventory().addItem(commandBook);
            }
        }
        user.setCommandBookItem(commandBook);
        Bukkit.getScheduler().runTaskLater(plugin, () -> CommandBook.updateCommandBook(plugin, user.player), 1L);
    }

    private static ItemStack createCommandBook(JavaPlugin plugin) {
        log.info("Player does not have command book, giving them one.");
        ItemStack commandBook = Utils.createItem(Material.WRITABLE_BOOK, 1, Utils.title("Command Book"), Utils.line("Open, type a command, then sign/close to run."),
                Utils.line("View chat history on other pages."));
        commandBook.editMeta(meta -> meta.getPersistentDataContainer().set(commandBookKey(plugin), PersistentDataType.BYTE, (byte) 1));
        return commandBook;
    }

    public static void removeCommandBook(NetworkUser user) {
        for (ItemStack item : user.player.getInventory().getContents()) {
            if (item != null && item.displayName().equals(TITLE)) {
                user.player.getInventory().remove(item);
                user.setCommandBookItem(null);
            }
        }
    }

    public static void updateCommandBook(Network plugin, Player player) {
        if (!player.isOnline()) {
            return;
        }
        NetworkUser user = plugin.getUser(player);
        if (user == null) {
            return;
        }

        List<Component> pages = new ArrayList<>();

        pages.add(
                Component.text("Write your command below the /, then sign or close the book to run it.")
                        .append(Component.newline())
                        .append(Component.text("Turn the page to view recent chat logs."))
                        .append(Component.newline())
                        .append(Component.newline())
                        .append(Component.text("/"))
        );

        Component page = Component.empty();

        int approxPlainChars = 0;
        int lineCount = 1; // header line

        for (Component originalMsg : user.getMessageHistory()) {
            String plain = PlainTextComponentSerializer.plainText().serialize(originalMsg).strip();
            if (plain.isBlank()) {
                continue;
            }

            Component msg = darkenForBookBackground(originalMsg);
            msg = normalizeForBook(msg);

            int msgLines = countLines(plain);
            int msgChars = plain.length();

            boolean wouldOverflow = (approxPlainChars + msgChars) > 250 || (lineCount + msgLines) > 13;
            if (wouldOverflow) {
                pages.add(page);
                page = Component.empty();
                approxPlainChars = 0;
                lineCount = 1;
            }

            page = page.append(msg).append(Component.newline());
            approxPlainChars += msgChars + 1;
            lineCount += msgLines;
        }

        if (approxPlainChars > 0) {
            pages.add(page);
        }

        ItemStack book = user.getCommandBookItem();
        BookMeta bookMeta = (BookMeta) book.getItemMeta();

        bookMeta = (BookMeta) bookMeta.pages(pages);

        book.setItemMeta(bookMeta);
    }

    /**
     * Book rendering can drop inherited formatting after embedded '\n' and sometimes around wrapping.
     * Normalize message components by:
     *  - splitting TextComponent content that contains '\n' into explicit segments + Component.newline()
     *  - making the effective color explicit on each segment (avoid relying on inheritance across lines)
     */
    private static Component normalizeForBook(Component in) {
        return explodeNewlinesAndFixColors(in, null);
    }

    private static Component explodeNewlinesAndFixColors(Component component, TextColor inheritedColor) {
        // Determine effective color at this node, then make it explicit (important for books)
        TextColor selfColor = component.color();
        TextColor effectiveColor = (selfColor != null) ? selfColor : inheritedColor;

        Component normalized = component;
        if (effectiveColor != null && selfColor == null) {
            normalized = normalized.color(effectiveColor);
        }

        // Recurse children
        List<Component> children = normalized.children();
        if (!children.isEmpty()) {
            List<Component> newChildren = new ArrayList<>(children.size());
            for (Component child : children) {
                newChildren.add(explodeNewlinesAndFixColors(child, effectiveColor));
            }
            normalized = normalized.children(newChildren);
        }

        // Split embedded newlines inside TextComponent content into explicit newline components
        if (normalized instanceof TextComponent text) {
            String content = text.content();
            if (content.indexOf('\n') >= 0) {
                String[] parts = content.split("\n", -1);

                Component rebuilt = Component.empty();
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        rebuilt = rebuilt.append(Component.newline());
                    }

                    // Preserve style + children of the original text component for each segment
                    // (children are already normalized above; we reuse them for each segment)
                    Component seg = Component.text(parts[i]).style(text.style()).children(text.children());

                    // Ensure the segment has an explicit effective color too
                    if (effectiveColor != null && seg.color() == null) {
                        seg = seg.color(effectiveColor);
                    }

                    rebuilt = rebuilt.append(seg);
                }
                return rebuilt;
            }
        }

        return normalized;
    }

    /**
     * Darken too-light colors for off-white book pages.
     * Works with both NamedTextColor and arbitrary RGB TextColor.
     * <p>
     * Implementation note: some Adventure versions don't expose Component#toBuilder(),
     * so we rebuild immutably using the available Component methods.
     */
    private static Component darkenForBookBackground(Component in) {
        return darkenComponentTree(in);
    }

    private static Component darkenComponentTree(Component component) {
        // Recurse into children first
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            List<Component> newChildren = new ArrayList<>(children.size());
            for (Component child : children) {
                newChildren.add(darkenComponentTree(child));
            }
            component = component.children(newChildren);
        }

        // Then adjust this component's style color (if any)
        Style style = component.style();
        TextColor mapped = mapToDarker(style.color());
        if (mapped != null) {
            component = component.style(style.color(mapped));
        }

        return component;
    }

    private static TextColor mapToDarker(TextColor colour) {
        if (colour == null) {
            colour = NamedTextColor.WHITE;
        }

        // If it's too bright, darken it by scaling RGB.
        int rgb = colour.value();
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        // Relative luminance (simple sRGB approximation)
        double luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0;

        // Only darken very bright colours.
        if (luminance < 0.78) {
            return null;
        }

        double factor = 0.65; // lower = darker
        int nr = clamp255((int) Math.round(r * factor));
        int ng = clamp255((int) Math.round(g * factor));
        int nb = clamp255((int) Math.round(b * factor));

        // If we didn't change anything, skip
        if (nr == r && ng == g && nb == b) {
            return null;
        }

        return TextColor.color(nr, ng, nb);
    }

    private static int clamp255(int value) {
        return Math.clamp(value, 0,255);
    }

    private static int countLines(String plain) {
        int lines = 1;
        for (int i = 0; i < plain.length(); i++) {
            if (plain.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }
}
