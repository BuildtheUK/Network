package net.bteuk.network.gui;

import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.entity.NetworkLocation;
import net.bteuk.network.commands.Navigator;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.gui.plotsystem.PlotMenu;
import net.bteuk.network.gui.plotsystem.PlotServerLocations;
import net.bteuk.network.gui.plotsystem.PlotsystemLocations;
import net.bteuk.network.gui.plotsystem.ZoneMenu;
import net.bteuk.network.gui.regions.RegionInfo;
import net.bteuk.network.gui.regions.RegionMenu;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.regions.Region;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionStatus;
import net.bteuk.network.regions.RegionUser;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.Optional;

public class BuildGui extends NetworkRefreshableGui {

    private final NetworkUser user;
    private final PlotSQL plotSQL;
    private final Constants constants;
    private final Back back;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    private final RegionManager regionManager;
    private final Navigator navigator;

    public BuildGui(GuiProvider provider, NetworkUser user) {
        super(provider, 27, Component.text("Building Menu", NamedTextColor.AQUA, TextDecoration.BOLD));
        this.user = user;
        this.plotSQL = provider.plotSQL();
        this.constants = provider.constants();
        this.back = provider.back();
        this.eventAPI = provider.eventAPI();
        this.serverAPI = provider.serverAPI();
        this.regionManager = provider.regionManager();
        this.navigator = provider.navigator();
    }

    protected void createGui() {

        // Teleport to random unclaimed plot.
        if (constants.plotSystemEnabled()) {
            setItem(20, Utils.createItem(Material.ENDER_PEARL, 1, Utils.title("Random Plot"), Utils.line("Click teleport to a random claimable plot."),
                            Utils.line("Available plots of each difficulty:"), Utils.line("Easy: ")
                                    .append(Component.text(plotSQL.getInt("SELECT count(id) FROM plot_data WHERE status='unclaimed' AND difficulty=1;"), NamedTextColor.GRAY)),
                            Utils.line("Normal: ")
                                    .append(Component.text(plotSQL.getInt("SELECT count(id) FROM plot_data WHERE status='unclaimed' AND difficulty=2;"), NamedTextColor.GRAY)),
                            Utils.line("Hard: ")
                                    .append(Component.text(plotSQL.getInt("SELECT count(id) FROM plot_data WHERE status='unclaimed' AND difficulty=3;"),
                                            NamedTextColor.GRAY))),
                    (NetworkUser u) -> {

                        int id;

                        if (u.player.hasPermission("uknet.plots.suggested.all")) {

                            // Select a random plot of any difficulty.
                            id = plotSQL.getInt("SELECT id FROM plot_data WHERE status='unclaimed' ORDER BY RAND() LIMIT 1;");
                        } else if (u.player.hasPermission("uknet.plots.suggested.hard")) {

                            // Select a random plot of the hard difficulty.
                            // Since this is the next plot difficulty to get Builder.
                            id = plotSQL.getInt("SELECT id FROM plot_data WHERE status='unclaimed' AND difficulty=3 ORDER BY RAND() LIMIT 1;");
                        } else if (u.player.hasPermission("uknet.plots.suggested.normal")) {

                            // Select a random plot of the normal difficulty.
                            // Since this is the next plot difficulty to get Jr.Builder.
                            id = plotSQL.getInt("SELECT id FROM plot_data WHERE status='unclaimed' AND difficulty=2 ORDER BY RAND() LIMIT 1;");
                        } else if (u.player.hasPermission("uknet.plots.suggested.easy")) {

                            // Select a random plot of the easy difficulty.
                            // Since this is the next plot difficulty to get Apprentice.
                            id = plotSQL.getInt("SELECT id FROM plot_data WHERE status='unclaimed' AND difficulty=1 ORDER BY RAND() LIMIT 1;");
                        } else {

                            // Select a random plot of any difficulty.
                            id = plotSQL.getInt("SELECT id FROM plot_data WHERE status='unclaimed' ORDER BY RAND() LIMIT 1;");
                        }

                        if (id == 0) {

                            u.player.sendMessage(ChatUtils.error("There are no plots available, please wait for new plots to be added."));
                            u.player.closeInventory();
                        } else {

                            // Get the server of the plot.
                            String server = plotSQL.getString(
                                    "SELECT server FROM location_data WHERE name=(SELECT location FROM plot_data WHERE id=?);", id);

                            // If the plot is on the current server teleport them directly.
                            // Else teleport them to the correct server and them teleport them to the plot.
                            NetworkLocation location = LocationAdapter.adapt(u.player.getLocation());
                            if (server.equals(constants.serverName())) {

                                u.player.closeInventory();

                                // Set current location for /back
                                provider.previousLocationTracker().setPreviousCoordinate(u.player.getUniqueId().toString(), location);

                                eventAPI.createTeleportEvent(false, u.player.getUniqueId().toString(), "plotsystemteleport plot " + id, location);
                            } else {
                                u.player.closeInventory();

                                // Set the server join event.
                                eventAPI.createTeleportEvent(true, u.player.getUniqueId().toString(), "plotsystemteleport plot " + id, location);

                                // Teleport them to another server.
                                serverAPI.switchServer(PlayerAdapter.adapt(u.player), server);
                            }
                        }
                    });

            // Choose location.
            setItem(19, Utils.createItem(Material.DIAMOND_PICKAXE, 1, Utils.title("Plot Locations"), Utils.line("Click to choose a location to build a plot.")),
                    (NetworkUser u) -> {
                        // Delete this gui.
                        this.delete();

                        // Switch to the plot location gui.
                        u.mainGui = new PlotServerLocations(provider, u);
                        u.mainGui.open(u.player);
                    });

            // Plot menu.
            setItem(21, Utils.createItem(Material.CHEST, 1, Utils.title("Plot Menu"), Utils.line("View all your active plots.")), (NetworkUser u) -> {
                // Delete this gui.
                this.delete();

                // Switch to the plot menu.
                u.mainGui = new PlotMenu(provider, u);
                u.mainGui.open(u.player);
            });

            // Zone menu.
            setItem(17, Utils.createItem(Material.BARREL, 1, Utils.title("Zone Menu"), Utils.line("View all zones you can build in.")), (NetworkUser u) -> {
                // Must have zone joining perms to open this menu.
                if (u.player.hasPermission("uknet.zones.join")) {

                    // Delete this gui.
                    this.delete();

                    // Switch to the zone menu.
                    u.mainGui = new ZoneMenu(provider, u);
                    u.mainGui.open(u.player);
                } else {

                    u.player.sendMessage(ChatUtils.error("You must be at least " + constants.minrankZoneJoin() + " to join zones."));
                }
            });

            // Menu to teleport to plotsystem locations without going through a plot selection process.
            setItem(22, Utils.createItem(Material.MINECART, 1, Utils.title("Plotsystem Locations"), Utils.line("Teleport to a location"), Utils.line("used by the Plotsystem.")),
                    (NetworkUser u) -> {

                        this.delete();

                        u.mainGui = new PlotsystemLocations(provider);
                        u.mainGui.open(u.player);
                    });
        }

        // Claim plot
        // This button only appears when in a plot server, else it'll show the region button.
        if (constants.serverType() == ServerType.PLOT && constants.plotSystemEnabled()) {
            setItem(4, Utils.createItem(Material.EMERALD, 1, Utils.title("Claim Plot"), Utils.line("Click to claim the plot you are currently standing in.")), (NetworkUser u) -> {

                // Set the claim event.
                u.player.closeInventory();
                eventAPI.createEvent(u.player.getUniqueId().toString(), constants.serverName(), "claim plot");
            });
        } else if (constants.regionsEnabled()) {

            /*
            Region Join Button

            Claimable:
            -   No active owner
                - uknet.regions.staff_request.always: Always requires request
                - uknet.regions.staff_request.bypass: Join region without request.
                - Check radius if any nearby region is claimed.
            -   Has active owner
                - Default (Owner request)
                - Public (No request needed)

            */

            // Join region (Users with uknet.regions.join only)
            // If region is claimable.
            // Check if the player is in a region.
            Optional<RegionUser> optionalRegionUser = regionManager.getUserByPlayer(user.player);
            if (optionalRegionUser.isPresent() && optionalRegionUser.get().hasTrackedRegion()) {
                Region region = optionalRegionUser.get().getTrackedRegion();

                // Check if you're an owner or member of this region.
                // If true then open the region info menu instead.
                // If you're already waiting for you request to be reviewed then show that.
                if (regionManager.isOwner(region, user.player.getUniqueId().toString())) {

                    setItem(4, Utils.createItem(Material.LIME_GLAZED_TERRACOTTA, 1, Utils.title("Region " + regionManager.getTag(region, user.player.getUniqueId().toString())),
                            Utils.line("You are the owner of this region."), Utils.line("Click to open the menu of this region.")), (NetworkUser u) -> {

                        // Delete this gui.
                        this.delete();

                        // Switch to region info.
                        u.mainGui = new RegionInfo(provider, region, u.player.getUniqueId().toString());
                        u.mainGui.open(u.player);
                    });
                } else if (regionManager.isMember(region, user.player.getUniqueId().toString())) {

                    setItem(4, Utils.createItem(Material.YELLOW_GLAZED_TERRACOTTA, 1, Utils.title("Region " + regionManager.getTag(region, user.player.getUniqueId().toString())),
                            Utils.line("You are a member of this region."), Utils.line("Click to open the menu of this plot.")), (NetworkUser u) -> {

                        // Delete this gui.
                        this.delete();

                        // Switch to plot info.
                        u.mainGui = new RegionInfo(provider, region, u.player.getUniqueId().toString());
                        u.mainGui.open(u.player);
                    });
                } else if (regionManager.hasRequest(region, user.getUuid())) {

                    setItem(4, Utils.createItem(Material.ORANGE_GLAZED_TERRACOTTA, 1, Utils.title("Region " + regionManager.getTag(region, user.player.getUniqueId().toString())),
                                    Utils.line("You have requested to join this region."), Utils.line("The request is still pending."), Utils.line("Click to cancel the request.")),
                            (NetworkUser u) -> {

                                // Close the gui.
                                u.player.closeInventory();

                                // Cancel the request.
                                regionManager.cancelRequest(region, u.player);
                            });
                } else if (user.player.hasPermission("uknet.regions.join")) {

                    // Check if the region is claimable.
                    if (regionManager.isClaimable(region)) {

                        boolean hasOwner = regionManager.hasActiveOwner(region);
                        String owner = (hasOwner) ? regionManager.ownerName(region) : "noone";
                        boolean alwaysStaffApproval = user.player.hasPermission("uknet.regions.staff_request.always");

                        // If the region has an owner.
                        if (hasOwner) {

                            if (alwaysStaffApproval)
                                setItem(4, Utils.createItem(Material.DARK_OAK_DOOR, 1, Utils.title("Join Region"), Utils.line("Click to join the region you are standing in."),
                                        Utils.line("The region is owned by ").append(Component.text(owner, NamedTextColor.GRAY)),
                                        Utils.line("They must accept the request for you to join."),
                                        Utils.line("You must also receive staff approval to join this region.")), (NetworkUser u) -> {
                                    regionManager.requestRegion(region, u.player, RegionManager.RequestType.BOTH);
                                    u.player.closeInventory();
                                });

                                // Check if the region is public.
                            else if (regionManager.status(region) == RegionStatus.PUBLIC)
                                setItem(4, Utils.createItem(Material.DARK_OAK_DOOR, 1, Utils.title("Join Region"), Utils.line("Click to join the region you are standing in."),
                                        Utils.line("The region is owned by ").append(Component.text(owner, NamedTextColor.GRAY)),
                                        Utils.line("The region is public, so they don't need to accept your request.")), (NetworkUser u) -> {
                                    regionManager.joinRegion(region, u.player);
                                    u.player.closeInventory();
                                });

                            else
                                // Join requires owner to approve request.
                                setItem(4, Utils.createItem(Material.DARK_OAK_DOOR, 1, Utils.title("Join Region"),
                                        Utils.line("Click to request to join the region you are standing in."),
                                        Utils.line("The region is owned by ").append(Component.text(owner, NamedTextColor.GRAY)),
                                        Utils.line("They must accept the request for you to join.")), (NetworkUser u) -> {

                                    regionManager.requestRegion(region, u.player, RegionManager.RequestType.OWNER);
                                    u.player.closeInventory();
                                });
                        } else { // No Owner

                            boolean staffApproval = alwaysStaffApproval;
                            // If not automatic staff approval, and the player does not have the bypass permission
                            // Check if the region was previously claimed or staff request is always required
                            // Check if any nearby regions are claimed by someone else.
                            // If true then the region needs to be checked by a staff member.
                            if (!staffApproval && !user.player.hasPermission("uknet.regions.staff_request.bypass")) {

                                staffApproval = constants.regionStaffRequestAlways() || regionManager.wasClaimed(region);

                                // If still not requiring staff approval, check the neighbour regions
                                if (!staffApproval) {

                                    // Get region coords.
                                    int x = Integer.parseInt(region.regionName().split(",")[0]);
                                    int z = Integer.parseInt(region.regionName().split(",")[1]);

                                    // Get the radius.
                                    int radius = constants.regionStaffRequestRadius();

                                    // For zero radius, skip.
                                    if (radius != 0) {

                                        // Subtract the config radius value.
                                        x -= radius;
                                        z -= radius;

                                        // Iterate through all regions in the radius.
                                        for (int i = x; (i <= x + radius * 2) && !staffApproval; i++) {
                                            for (int j = z; (j <= z + radius * 2) && !staffApproval; j++) {

                                                String regionName = i + "," + j;

                                                // If the region exists, check if it has an owner that is
                                                // not the player.
                                                if (regionManager.exists(regionName)) {
                                                    Region regionInRadius = regionManager.getRegion(regionName);
                                                    if (regionManager.hasOwner(regionInRadius)) {
                                                        if (!regionManager.getOwner(regionInRadius).equals(user.player.getUniqueId().toString())) {
                                                            // Staff approval is required.
                                                            staffApproval = true;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (staffApproval)

                                // Join region.
                                setItem(4, Utils.createItem(Material.DARK_OAK_DOOR, 1, Utils.title("Join Region"), Utils.line("Click to join the region you are standing in."),
                                        Utils.line("The region currently has no active owner."), Utils.line("Joining the region will make you the region owner."),
                                        Utils.line("You will require staff approval to join this region.")), (NetworkUser u) -> {
                                    regionManager.requestRegion(region, u.player, RegionManager.RequestType.STAFF);
                                    u.player.closeInventory();
                                });
                            else

                                setItem(4, Utils.createItem(Material.DARK_OAK_DOOR, 1, Utils.title("Join Region"), Utils.line("Click to join the region you are standing in."),
                                                Utils.line("The region currently has no active owner."), Utils.line("Joining the region will make you the region owner.")),
                                        (NetworkUser u) -> {
                                            regionManager.joinRegion(region, u.player);
                                            u.player.closeInventory();
                                        });
                        }
                    } else {

                        // If the region is open.
                        if (regionManager.status(region) == RegionStatus.OPEN) {
                            setItem(4, Utils.createItem(Material.SPYGLASS, 1, Utils.title("Open Region"), Utils.line("This region is open to all Jr.Builder+."),
                                    Utils.line("They can build here without claiming.")));
                        } else {

                            // This region is not claimable.
                            setItem(4, Utils.createItem(Material.IRON_DOOR, 1, Utils.title("Locked Region"), Utils.line("This region can not be claimed."),
                                    Utils.line("It is either locked or used in the plot system.")));
                        }
                    }
                } else {

                    // Can't claim since you don't have regions.join.
                    setItem(4, Utils.createItem(Material.STRUCTURE_VOID, 1, Utils.title("Unable to Join Region"), Utils.line("To be able to join a region you"),
                            Utils.line("must gain at least " + constants.minrankRegionClaim() + " or above."),
                            Utils.line("For more information type ").append(Component.text("/help building", NamedTextColor.GRAY))));
                }
            } else {
                // Show that the user is not in a region.
                setItem(4, Utils.createItem(Material.STRUCTURE_VOID, 1, Utils.title("No Region"), Utils.line("You are currently not standing in a valid region."),
                        Utils.line("This is likely due to being in a lobby.")));
            }
        }

        if (constants.regionsEnabled()) {
            // Region menu.
            setItem(24, Utils.createItem(Material.ORANGE_SHULKER_BOX, 1, Utils.title("Region Menu"), Utils.line("View all regions you can build in.")), (NetworkUser u) -> {
                this.delete();

                // Switch to the region menu.
                u.mainGui = new RegionMenu(provider, u);
                u.mainGui.open(u.player);
            });
        }

        // Building utils menu.
        setItem(8, Utils.createItem(Material.NETHERITE_AXE, 1, Utils.title("Building Utils"), Utils.line("Open the building utils menu.")), (NetworkUser u) -> {
            this.delete();
            u.mainGui = new UtilsGui(provider);
            u.mainGui.open(u.player);
        });

        // if (constants.progressMap() && user.player.hasPermission("uknet.progressmap.edit")) {
        //     // Progress map edit menu
        //     setItem(0, Utils.createItem(Material.MAP, 1, Utils.title("Progress Map"), Utils.line("Edit or add areas to the progress map")), (NetworkUser u) -> {
        //
        //         LocalFeaturesMenu localFeatures = new LocalFeaturesMenu(constants.progressMapID(), constants.mapHubAPIKey(), u.player);
        //
        //         // Check to see if the location could be established
        //         if (localFeatures.getPlayerCoordinates() == null) {
        //             u.player.sendMessage(ChatUtils.error("Could not locate you"));
        //         } else {
        //             this.delete();
        //             // Switch to the local features menu
        //             u.mainGui = new LocalFeatureListGUI(provider, localFeatures, localFeatures.getGUI());
        //             u.mainGui.open(u.player);
        //         }
        //     });
        // }

        // Return
        setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the navigator main menu.")), (NetworkUser u) -> {

            // Delete this gui.
            this.delete();

            // Switch to the navigation menu.
            navigator.openMainMenu(u);
        });
    }
}
