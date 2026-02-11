package net.bteuk.network.chat.bypass;

import net.bteuk.minecraft.texteditorbooks.BookCloseAction;
import net.bteuk.minecraft.texteditorbooks.TextEditorBookListener;
import net.bteuk.network.Network;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Executes a command typed in a writable book and provides actionbar feedback.
 */
public class CommandBookCloseAction implements BookCloseAction {

    private final Network instance;
    private final NetworkUser user;

    public CommandBookCloseAction(Network instance, NetworkUser user) {
        this.instance = instance;
        this.user = user;
    }

    @Override
    public boolean runBookClose(BookMeta oldBookMeta, BookMeta newBookMeta, TextEditorBookListener listener, String newContent) {
        // Since we now have pagination, newContent is the concatenated text of all pages.
        // The command is expected to be on the first page, after the instruction text and /.

        if (newBookMeta.pages().isEmpty()) {
            user.player.sendActionBar(Component.text("No command entered"));
            listener.unregister();
            return false;
        }

        // We use the plain text version to find the index to avoid issues with formatting codes.
        String plainFirstPage = PlainTextComponentSerializer.plainText().serialize(newBookMeta.page(1));

        // Instructions and separator are on the first page.
        // We look for the / and take everything after it.
        int index = plainFirstPage.indexOf("/");
        String temp = "";
        if (index != -1) {
            temp = plainFirstPage.substring(index + 1).trim();
        }

        final String finalCmd = temp;
        if (!finalCmd.isEmpty()) {
            // Dispatch as the player
            Bukkit.getScheduler().runTask(instance, () -> Bukkit.dispatchCommand(user.player, finalCmd));
        } else {
            user.player.sendActionBar(Component.text("No command entered"));
        }
        // Unregister the temporary listener now that we're done
        listener.unregister();
        return false; // Do not persist the book pages by default
    }

    @Override
    public boolean runBookSign(BookMeta bookMeta, BookMeta bookMeta1, TextEditorBookListener textEditorBookListener, String s) {
        return this.runBookClose(bookMeta, bookMeta1, textEditorBookListener, s);
    }

    @Override
    public void runPostClose() {
        // After closing, remove the temporary editor book from the hotbar.
        for (int i = 0; i < 9; i++) {
            ItemStack stack = user.player.getInventory().getItem(i);
            if (stack != null && stack.getType() == Material.WRITABLE_BOOK) {
                if (stack.hasItemMeta() && stack.getItemMeta() instanceof BookMeta meta && meta.title() != null && meta.title().equals("Command Book")) {
                    user.player.getInventory().setItem(i, null);
                }
            }
        }
    }
}
