package net.bteuk.network.gui.staff;

import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.ModerationType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

public class ModerationGui extends Gui {

    public ModerationGui() {

        super(27, Component.text("Moderation Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        createGui();
    }

    private void createGui() {

        // Ban
        setItem(10, Utils.createCustomSkullWithFallback(
                        "30506c52de360dfaec1b84998ba060fa6ce12be818fc13edc5db7a7921a35d7e", Material.REDSTONE_BLOCK, 1,
                        Utils.title("Ban"),
                        Utils.line("Click to select an online user to ban.")),
                (NetworkUser u) -> {

                    if (u.hasPermission("uknet.ban")) {
                        openSelectUser(u, ModerationType.BAN);
                    }
                });

        // Unban
        setItem(15, Utils.createCustomSkullWithFallback(
                        "c2abe43288a6c8cd76d0228f39112d2520c289d7c15c6aafe0c532ad9f5db9ad", Material.REDSTONE_BLOCK, 1,
                        Utils.title("Unban"),
                        Utils.line("Click to select a banned user to unban.")),
                (NetworkUser u) -> {

                    if (u.hasPermission("uknet.ban")) {
                        openSelectUser(u, ModerationType.UNBAN);
                    }
                });

        // Mute
        setItem(11, Utils.createCustomSkullWithFallback(
                        "4f130f485c3f7697f320ddc1128cd3f17cdbd3791764f7a7bb95cf252738588", Material.REDSTONE_BLOCK, 1,
                        Utils.title("Mute"),
                        Utils.line("Click to select an online user to mute.")),
                (NetworkUser u) -> {

                    if (u.hasPermission("uknet.mute")) {
                        openSelectUser(u, ModerationType.MUTE);
                    }
                });

        // Unmute
        setItem(16, Utils.createCustomSkullWithFallback(
                        "f81422e8ddc0d3109aa657b89b0b0eb1d25cb3bc8d54dc6c99c3c9c081440254", Material.REDSTONE_BLOCK, 1,
                        Utils.title("Unmute"),
                        Utils.line("Click to select a muted user to unmute.")),
                (NetworkUser u) -> {

                    if (u.hasPermission("uknet.mute")) {
                        openSelectUser(u, ModerationType.UNMUTE);
                    }
                });

        // Kick
        setItem(13, Utils.createCustomSkullWithFallback(
                        "5ae0e486db4ec49ff1b52cfeceda4c3f36fde23c835ea3ccfcaac935e49b5f10", Material.REDSTONE_BLOCK, 1,
                        Utils.title("Kick"),
                        Utils.line("Click to select an online user to kick.")),
                (NetworkUser u) -> {

                    if (u.hasPermission("uknet.kick")) {
                        openSelectUser(u, ModerationType.KICK);
                    }
                });

        // Return to moderation menu.
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Previous Page"),
                        Utils.line("Open the staff menu.")),
                u ->

                {

                    // Return to request menu.
                    this.delete();
                    u.staffGui = new StaffGui(u);
                    u.staffGui.open(u.player);
                });
    }

    public void refresh() {

        this.clearGui();
        createGui();
    }

    private void openSelectUser(NetworkUser u, ModerationType type) {

        // Switch to the select user menu.
        this.delete();

        u.staffGui = new SelectUser(type);
        u.staffGui.open(u.player);
    }
}
