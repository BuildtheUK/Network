package net.bteuk.network.gui.plotsystem;

import net.bteuk.network.api.plotsystem.ZoneMembership;
import net.bteuk.network.api.plotsystem.ZoneOwner;
import net.bteuk.network.gui.BuildGui;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ZoneMenu extends NetworkRefreshableGui {

    private final NetworkUser user;
    private final PlotSQL plotSQL;

    private ArrayList<Integer> zones;
    private final ArrayList<ZoneType> zoneTypes = new ArrayList<>();
    private final ArrayList<String> ownerNames = new ArrayList<>();

    public ZoneMenu(GuiProvider provider, NetworkUser user) {

        super(provider, 45, Component.text("Zone Menu", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.user = user;
        this.plotSQL = provider.plotSQL();
    }

    @Override
    protected void loadData() {
        // Get all zones that are open.
        this.zones = plotSQL.getIntList("SELECT id FROM zones WHERE status='open';");

        zoneTypes.clear();
        ownerNames.clear();
        if (zones != null && !zones.isEmpty()) {
            String ids = zones.toString().replace("[", "(").replace("]", ")");

            // Fetch membership info for the current user for all these zones.
            List<ZoneMembership> zoneMemberships = plotSQL.getZoneMemberships(user.player.getUniqueId().toString(), ids);

            // Fetch public status for all these zones.
            List<Integer> publicZones = plotSQL.getIntList("SELECT id FROM zones WHERE is_public=1 AND id IN " + ids + ";");

            // Fetch owners for zones that might be private.
            List<ZoneOwner> zoneOwners = plotSQL.getZoneOwners(ids);

            for (int zoneId : zones) {
                Optional<ZoneMembership> membership = zoneMemberships.stream().filter(m -> m.id() == zoneId).findFirst();
                if (membership.isPresent()) {
                    if (membership.get().isOwner()) {
                        zoneTypes.add(ZoneType.OWNER);
                    } else {
                        zoneTypes.add(ZoneType.MEMBER);
                    }
                    ownerNames.add(null);
                } else if (publicZones.contains(zoneId)) {
                    zoneTypes.add(ZoneType.PUBLIC);
                    ownerNames.add(null);
                } else {
                    zoneTypes.add(ZoneType.PRIVATE);
                    Optional<ZoneOwner> owner = zoneOwners.stream().filter(o -> o.id() == zoneId).findFirst();
                    String ownerUuid = owner.map(ZoneOwner::uuid).orElse(null);
                    ownerNames.add(ownerUuid != null ? provider.globalSQL().getString("SELECT name FROM player_data WHERE uuid='" + ownerUuid + "';") : "Unknown");
                }
            }
        }
    }

    protected void createGui() {

        /*
        Gui layout:

        List all zones that you are a member of, then all public zones that can be joined.

        Return button in the last slot.

         */

        // Slot count.
        int slot = 10;

        // Make a button for each plot.
        if (this.zones != null) {
            for (int i = 0; i < this.zones.size(); i++) {

                int finalI = i;
                ZoneType type = zoneTypes.get(i);

                // If you are the zone owner, or a member, open the zone info menu.
                // If the zone is public then join the zone by clicking.
                // If the zone is private, do nothing.
                if (type == ZoneType.OWNER || type == ZoneType.MEMBER) {

                    setItem(slot, Utils.createItem(
                                    (type == ZoneType.OWNER ? Material.LIME_CONCRETE : Material.YELLOW_CONCRETE),
                                    1,
                                    Utils.title("Zone " + this.zones.get(i)),
                                    Utils.line("Click to open the menu of this zone.")),
                            (NetworkUser u) -> {

                                // Delete this gui.
                                this.delete();
                                u.mainGui = null;

                                // Switch to zone info.
                                u.mainGui = new ZoneInfo(provider, u, this.zones.get(finalI), u.player.getUniqueId().toString());
                                u.mainGui.open(u.player);
                            });
                } else if (type == ZoneType.PUBLIC) {

                    setItem(slot, Utils.createItem(Material.LIGHT_BLUE_CONCRETE,
                                    1,
                                    Utils.title("Zone " + this.zones.get(i)),
                                    Utils.line("Click to join this zone.")),
                            (NetworkUser u) -> {

                                // Add server event to join zone.
                                // This involves a sync DB query for location/server, so offload it.
                                provider.instance().getServer().getScheduler().runTaskAsynchronously(provider.instance(), () -> {
                                    String server = plotSQL.getString("SELECT server FROM location_data WHERE name='" +
                                            plotSQL.getString("SELECT location FROM zones WHERE id=" + this.zones.get(
                                                    finalI) + ";") + "';");

                                    provider.eventAPI().createEvent(u.player.getUniqueId().toString(), server,
                                            "join zone " + this.zones.get(finalI));

                                    // Close inventory to prevent double clicking.
                                    provider.instance().getServer().getScheduler().runTask(provider.instance(), () -> u.player.closeInventory());
                                });
                            });
                } else {

                    setItem(slot, Utils.createItem(Material.BARRIER,
                            1,
                            Utils.title("Zone " + this.zones.get(i)),
                            Utils.line("This zone is private,"),
                            Utils.line("to join this zone you must be"),
                            Utils.line("invited by ")
                                    .append(Component.text(ownerNames.get(i), NamedTextColor.GRAY))));
                }

            // Increase slot accordingly.
            if (slot % 9 == 7) {
                // Increase row, basically add 3.
                slot += 3;
            } else {
                // Increase value by 1.
                slot++;
            }
        }

        // Return
        setItem(44, Utils.createItem(Material.SPRUCE_DOOR, 1,
                        Utils.title("Return"),
                        Utils.line("Open the building menu.")),
                (NetworkUser u) -> {

                    // Delete this gui.
                    this.delete();
                    u.mainGui = null;

                    // Switch to plot info.
                    u.mainGui = new BuildGui(provider, u);
                    u.mainGui.open(u.player);
                });
        }
    }
    private enum ZoneType {
        OWNER,
        MEMBER,
        PUBLIC,
        PRIVATE
    }
}
