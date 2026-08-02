package net.bteuk.network.gui.staff;

import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionStatus;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Material;

public class ManageRegion extends NetworkRefreshableGui {

    private final Region region;
    private final NetworkUser user;
    private final RegionManager regionManager;

    public ManageRegion(GuiProvider provider, NetworkUser user, Region region) {
        super(provider, 9, Component.text("Manage Region", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.region = region;
        this.user = user;
        this.regionManager = provider.regionManager();
    }

    protected void createGui() {

        setItem(0, Utils.createItem(Material.ENCHANTED_BOOK, 1, Utils.title("Region " + region.regionName()),
                Utils.line("Current owner: ").append(Component.text(regionManager.ownerName(region), NamedTextColor.GRAY)),
                Utils.line("Number of members: ").append(Component.text(regionManager.memberCount(region), NamedTextColor.GRAY)),
                Utils.line("Region status: ").append(Component.text(regionManager.status(region).label, NamedTextColor.GRAY))));

        // Set public if the status is default or inactive.
        // Set private if the status is public.
        if (user.player.hasPermission("uknet.regions.manage.public")) {
            if (regionManager.status(region) == RegionStatus.DEFAULT || regionManager.status(region) == RegionStatus.INACTIVE) {

                setItem(2, Utils.createItem(Material.OAK_DOOR, 1, Utils.title("Make region public"), Utils.line("A public region allows members"),
                                Utils.line("to join without needing"), Utils.line("the owner to accept it.")),

                        (NetworkUser u) -> {

                            regionManager.setPublic(region);
                            u.player.sendMessage(ChatUtils.success("Set region ").append(Component.text(region.regionName(), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" to public.")));
                            this.refresh();
                        });
            } else if (regionManager.status(region) == RegionStatus.PUBLIC) {

                setItem(2, Utils.createItem(Material.IRON_DOOR, 1, Utils.title("Make region private"), Utils.line("The default region setting,"),
                                Utils.line("joining requires the owner"), Utils.line("to accept the request.")),

                        (NetworkUser u) -> {

                            regionManager.setDefault(region);
                            u.player.sendMessage(ChatUtils.success("Set region ").append(Component.text(region.regionName(), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" to default.")));
                            this.refresh();
                        });
            }
        }

        // Transfer ownership if status is default or public, must have at least 1 member.
        if (user.player.hasPermission("uknet.regions.manage.owner")) {
            if (regionManager.hasMember(region)) {

                // Slot 5
                setItem(5, Utils.createItem(Material.MAGENTA_GLAZED_TERRACOTTA, 1, Utils.title("Transfer Ownership"), Utils.line("Open the transfer ownership menu."),
                                Utils.line("Allows you to make a member"), Utils.line("the new region owner.")),

                        (NetworkUser u) -> {

                            // Close this menu.
                            this.delete();

                            // Open the transfer owner menu.
                            u.staffGui = new TransferOwner(provider, region);
                            u.staffGui.open(u.player);
                        });
            }
        }

        // Kick members; must have an owner and/or members.
        if (user.player.hasPermission("uknet.regions.manage.kick")) {
            if (regionManager.hasOwner(region) || regionManager.hasMember(region)) {

                // Slot 6
                setItem(6, Utils.createItem(Material.BARRIER, 1, Utils.title("Kick Members"), Utils.line("Remove any current members,"),
                        Utils.line("or the owner from the regionManager.")), (NetworkUser u) -> {

                    // Close this menu.
                    this.delete();

                    // Open the transfer owner menu.
                    u.staffGui = new KickMembers(provider, region);
                    u.staffGui.open(u.player);
                });
            }
        }

        // Set region locked if the region is default, public, open or inactive.
        // Set region unlocked if the region is locked.
        if (user.player.hasPermission("uknet.regions.manage.lock")) {
            if (regionManager.status(region) == RegionStatus.DEFAULT || regionManager.status(region) == RegionStatus.PUBLIC || regionManager.status(
                    region) == RegionStatus.OPEN || regionManager.status(region) == RegionStatus.INACTIVE) {

                setItem(4, Utils.createItem(Material.IRON_TRAPDOOR, 1, Utils.title("Lock Region"), Utils.line("Locking a region stops anyone"),
                                Utils.line("from joining or building in the"), Utils.line("region, any existing members"), Utils.line("will be kicked")),

                        (NetworkUser u) -> {

                            // If the region is currently open, remove the jrbuilder group.
                            if (regionManager.status(region) == RegionStatus.OPEN) {
                                regionManager.setDefault(region, "jrbuilder");
                            }

                            regionManager.setLocked(region);
                            u.player.sendMessage(ChatUtils.success("Set Region ").append(Component.text(region.regionName(), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" to locked.")));

                            this.refresh();
                        });
            } else if (regionManager.status(region) == RegionStatus.LOCKED) {

                setItem(4, Utils.createItem(Material.OAK_TRAPDOOR, 1, Utils.title("Unlock Region"), Utils.line("The default region setting,"),
                                Utils.line("people will be able to join"), Utils.line("and build in the region again.")),

                        (NetworkUser u) -> {

                            regionManager.setDefault(region);
                            u.player.sendMessage(ChatUtils.success("Set region ").append(Component.text(region.regionName(), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" to default.")));
                            this.refresh();
                        });
            }
        }

        // Set region open if status is default, public or inactive.
        // Set region default if status is open.
        if (user.player.hasPermission("uknet.regions.manage.open")) {
            if (regionManager.status(region) == RegionStatus.DEFAULT || regionManager.status(region) == RegionStatus.PUBLIC || regionManager.status(
                    region) == RegionStatus.INACTIVE) {

                setItem(3, Utils.createItem(Material.OAK_FENCE_GATE, 1, Utils.title("Make region open"), Utils.line("An open region allows all"),
                                Utils.line("Jr.Builder+ to build without"), Utils.line("needing to join the regionManager."), Utils.line("Any existing members will be kicked.")),

                        (NetworkUser u) -> {
                            regionManager.setOpen(region);
                            u.player.sendMessage(
                                    ChatUtils.success("Set region ").append(Component.text(region.regionName(), NamedTextColor.DARK_AQUA)).append(ChatUtils.success(" to open.")));
                            this.refresh();
                        });
            } else if (regionManager.status(region) == RegionStatus.OPEN) {

                setItem(3, Utils.createItem(Material.OAK_FENCE, 1, Utils.title("Make region closed"), Utils.line("The default region setting,"),
                                Utils.line("people will again be required"), Utils.line("to join the region to build.")),

                        (NetworkUser u) -> {
                            regionManager.setDefault(region, "jrbuilder");
                            u.player.sendMessage(ChatUtils.success("Set region ").append(Component.text(region.regionName(), NamedTextColor.DARK_AQUA))
                                    .append(ChatUtils.success(" to default.")));
                            this.refresh();
                        });
            }
        }

        setItem(8, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Previous Page"), Utils.line("Open the staff menu.")), (NetworkUser u) ->
        {

            // Return to the staff menu.
            this.delete();
            u.staffGui = null;

            u.staffGui = new StaffGui(provider, u);
            u.staffGui.open(u.player);
        });
    }
}
