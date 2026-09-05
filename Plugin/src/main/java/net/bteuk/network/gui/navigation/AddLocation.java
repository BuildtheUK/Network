package net.bteuk.network.gui.navigation;

import lombok.Getter;
import lombok.Setter;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.eventing.listeners.navigation.LocationNameListener;
import net.bteuk.network.gui.GuiProvider;
import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.gui.staff.LocationRequests;
import net.bteuk.network.gui.staff.StaffGui;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.enums.AddLocationType;
import net.bteuk.network.utils.enums.Category;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.network.lib.dto.ChatMessage;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.Arrays;

import static org.btuk.network.lib.enums.ChatChannels.REVIEWER;

public class AddLocation extends NetworkRefreshableGui {

    @Getter
    private final AddLocationType type;
    private final Constants constants;
    private final Back back;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;
    public SelectSubcategory selectSubcategory;
    private String old_name;
    @Setter
    private String name;
    @Getter
    private Category category = Category.ENGLAND;
    @Setter
    private String subcategory = "None";
    private int coordinate_id;
    private LocationNameListener locationNameListener;
    private final GlobalSQL globalSQL;

    public AddLocation(GuiProvider provider, AddLocationType type) {
        super(provider, 27, Component.text(type.label + " Location", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.type = type;
        this.constants = provider.constants();
        this.back = provider.back();
        this.eventAPI = provider.eventAPI();
        this.serverAPI = provider.serverAPI();
        this.globalSQL = provider.globalSQL();
    }

    // This is used when location details need to be updated.
    public AddLocation(GuiProvider provider, AddLocationType type, String name, int coordinate_id, Category category, String subcategory) {
        super(provider, 27, Component.text(type.label + " Location", NamedTextColor.AQUA, TextDecoration.BOLD));

        // Set the name.
        this.old_name = name;
        this.name = name;

        // Set coordinate id.
        this.coordinate_id = coordinate_id;

        // Set category from input.
        this.category = category;

        if (subcategory != null) {
            this.subcategory = subcategory;
        }

        this.type = type;
        this.constants = provider.constants();
        this.back = provider.back();
        this.eventAPI = provider.eventAPI();
        this.serverAPI = provider.serverAPI();
        this.globalSQL = provider.globalSQL();
    }

    protected void createGui() {
        // Set/edit name.
        if (name != null) {
            setItem(11, Utils.createItem(Material.SPRUCE_SIGN, 1, Utils.title("Update Location Name"), Utils.line("Edit the location name."),
                            Utils.line("The current name is: ").append(Component.text(name, NamedTextColor.GRAY)), Utils.line("You can type the name in chat.")),

                    (NetworkUser u) -> {

                        if (locationNameListener != null) {
                            locationNameListener.unregister();
                        }

                        locationNameListener = new LocationNameListener(provider.instance(), u.player, this);
                        u.player.sendMessage(ChatUtils.success("Write the location name in chat, the first message " + "counts. You can include spaces in the name."));
                        u.player.closeInventory();
                    });
        } else {
            setItem(11, Utils.createItem(Material.SPRUCE_SIGN, 1, Utils.title("Set Location Name"), Utils.line("Add " + "the location name."),
                            Utils.line("You can type the name in chat.")),

                    (NetworkUser u) -> {

                        if (locationNameListener != null) {
                            locationNameListener.unregister();
                        }

                        locationNameListener = new LocationNameListener(provider.instance(), u.player, this);
                        u.player.sendMessage(ChatUtils.success("Write the location name in chat, the first message " + "counts. You can include spaces in the name."));
                        u.player.closeInventory();
                    });
        }

        // Select category.
        setItem(15, Utils.createItem(Material.MAP, 1, Utils.title("Select Category"), Utils.line("Click to cycle " + "through categories."),
                        Utils.line("Current category is: ").append(Component.text(category.getLabel(), NamedTextColor.GRAY)), Utils.line("Available categories are:"),
                        Utils.line("England, " + "Scotland, Wales, Northern Ireland and Other")),

                (NetworkUser u) -> {

                    // Cycle to next category and refresh the gui.
                    Category[] categories = Arrays.stream(Category.values()).filter(Category::isSelectable).toArray(Category[]::new);
                    for (int i = 0; i < categories.length; i++) {
                        if (categories[i] == category) {
                            // Get next.
                            if (i == categories.length - 1) {
                                category = categories[0];
                            } else {
                                category = categories[i + 1];
                            }
                            break;
                        }
                    }

                    // Update gui.
                    this.refresh();
                    this.updatePlayerInventory(u.player);
                });

        // Select subcategory.
        setItem(16,
                Utils.createItem(Material.COMPASS, 1, Utils.title("Select Subcategory"), Utils.line("Click to " + "select a subcategory."), Utils.line("This is optional, you can"),
                        Utils.line("leave it as " + "'None' if there"), Utils.line("are no suitable options"),
                        Utils.line("Current subcategory " + "is: ").append(Component.text(subcategory, NamedTextColor.GRAY))),

                (NetworkUser u) -> {

                    // Open select county menu.
                    selectSubcategory = new SelectSubcategory(provider, this);
                    selectSubcategory.open(u.player);
                });

        // Teleport location update.
        // Teleport to location.
        // These options are not needed for adding a location as that is based on the players current location.
        if (type != AddLocationType.ADD) {

            // Teleport to location.
            setItem(21, Utils.createItem(Material.ENDER_PEARL, 1, Utils.title("Teleport to Location"), Utils.line("Click to teleport to the location.")), (NetworkUser u) -> {

                // Close inventory.
                u.player.closeInventory();

                // If location is on this server teleport the player, else switch server.
                // Teleport to location.
                String server = globalSQL.getString("SELECT server FROM coordinates WHERE id=" + coordinate_id + ";");
                if (constants.serverName().equalsIgnoreCase(server)) {
                    // Get location from coordinate id.
                    Location l = globalSQL.getLocation(coordinate_id);

                    // Set current location for /back
                    provider.previousLocationTracker().setPreviousCoordinate(u.player.getUniqueId().toString(), LocationAdapter.adapt(u.player.getLocation()));

                    u.player.teleport(l);
                } else {
                    // Create teleport event and switch server.
                    u.player.closeInventory();
                    eventAPI.createTeleportEvent(true, u.player.getUniqueId().toString(), "teleport " + "location_request " + name,
                            LocationAdapter.adapt(u.player.getLocation()));
                    serverAPI.switchServer(PlayerAdapter.adapt(u.player), server);
                }
            });

            // Update teleport location.
            setItem(23, Utils.createItem(Material.ACACIA_BOAT, 1, Utils.title("Update teleport location"), Utils.line("Click to set the teleport"),
                    Utils.line("location to your current position.")), (NetworkUser u) -> {

                Location l = u.player.getLocation();

                // If server is plotsystem add the necessary coordinate transformation.
                if (constants.serverType() == ServerType.PLOT) {

                    String worldName = u.player.getLocation().getWorld().key().asMinimalString();

                    // If location exists.
                    if (provider.plotAPI().hasLocation(worldName)) {

                        // Add coordinate transformation.
                        l = new Location(l.getWorld(), l.getX() - provider.plotAPI().getXTransform(worldName), l.getY(), l.getZ() - provider.plotAPI().getZTransform(worldName),
                                l.getYaw(), l.getPitch());
                    }
                }

                globalSQL.updateCoordinate(coordinate_id, l);
                u.player.sendMessage(ChatUtils.success("Updated location to your current position."));
            });
        }

        /*
        Add location
        Accept if created by reviewer.
        Add location to database
        Add request to database
        Notify reviewers if online using reviewer chat channel
         */
        if (type == AddLocationType.ADD) {
            setItem(13, Utils.createItem(Material.EMERALD, 1, Utils.title("Add Location"), Utils.line("Your location " + "will be added to the exploration menu."),
                            Utils.line("However, it must first be accepted" + " by a reviewer.")),

                    (NetworkUser u) -> {

                        // Checks:
                        // Name has been set
                        if (name == null) {

                            u.player.sendMessage(ChatUtils.error("You have not set a name for the location."));
                            u.player.closeInventory();

                            // Name isn't duplicate (location or subcategory.
                        } else if (globalSQL.hasRow("SELECT location FROM location_data WHERE location='" + name + "';") || globalSQL.hasRow(
                                "SELECT name FROM location_subcategory WHERE name='" + name + "';")) {

                            u.player.sendMessage(ChatUtils.error("A location or subcategory with this name already " + "exists."));
                            u.player.closeInventory();
                        } else if (globalSQL.hasRow("SELECT location FROM location_requests WHERE location = '" + name + "';")) {

                            u.player.sendMessage(ChatUtils.error("A location with this name has already been " + "requested."));
                            u.player.closeInventory();
                        } else {

                            Location l = u.player.getLocation();

                            // If the server is the plot system, add the necessary coordinate transformation.
                            if (constants.serverType() == ServerType.PLOT) {

                                String worldName = u.player.getLocation().getWorld().key().asMinimalString();

                                // If location exists.
                                if (provider.plotSQL().hasRow("SELECT name FROM location_data WHERE " + "name='" + worldName + "';")) {

                                    // Add coordinate transformation.
                                    l = new Location(l.getWorld(),
                                            l.getX() - provider.plotSQL().getInt("SELECT xTransform " + "FROM location_data WHERE name='" + worldName + "';"), l.getY(),
                                            l.getZ() - provider.plotSQL().getInt("SELECT zTransform " + "FROM location_data WHERE name='" + worldName + "';"), l.getYaw(),
                                            l.getPitch());
                                }
                            }

                            // Create location coordinate.
                            coordinate_id = globalSQL.addCoordinate(l);

                            if (u.player.hasPermission("uknet.navigation.add")) {

                                addLocation(u);
                            } else {

                                requestLocation(u);
                            }
                        }
                    });
        } else if (type == AddLocationType.UPDATE) {
            setItem(4, Utils.createItem(Material.EMERALD, 1, Utils.title("Update Location"), Utils.line("The location" + " will be updated"),
                            Utils.line("with the selected settings.")),

                    (NetworkUser u) -> {

                        // Checks:
                        // Name isn't duplicate
                        if (globalSQL.hasRow("SELECT location FROM location_data WHERE location='" + name + " AND " + "coordinate<>" + coordinate_id + "';") || globalSQL.hasRow(
                                "SELECT name FROM " + "location_subcategory WHERE name='" + name + "';")) {

                            u.player.sendMessage(ChatUtils.error("Another location or subcategory with this name " + "already exists."));
                            u.player.closeInventory();
                        } else if (globalSQL.hasRow("SELECT location FROM location_requests WHERE location = '" + name + " AND coordinate" + "<>" + coordinate_id + "';")) {

                            u.player.sendMessage(ChatUtils.error("A location with this name has already been " + "requested."));
                            u.player.closeInventory();
                        } else {

                            updateLocation(u);
                        }
                    });
        } else if (type == AddLocationType.REVIEW) {

            // Accept the request.
            setItem(3, Utils.createItem(Material.LIME_CONCRETE, 1, Utils.title("Accept Location Request"), Utils.line("Location will be added to"),
                    Utils.line("the exploration menu as well as"), Utils.line("the list of warps.")), (NetworkUser u) -> {

                acceptRequest(u);

                // Delete gui and return to previous menu.
                this.delete();

                u.staffGui = new LocationRequests(provider);
                u.staffGui.open(u.player);
            });

            // Deny the request.
            setItem(5, Utils.createItem(Material.RED_CONCRETE, 1, Utils.title("Deny Location Request"), Utils.line("Location request will be denied.")), (NetworkUser u) -> {

                // Delete request.
                globalSQL.update("DELETE FROM location_requests WHERE location='" + name + "';");

                // Notify player.
                u.player.sendMessage(ChatUtils.error("Denied location request ").append(Component.text(name, NamedTextColor.DARK_RED)));

                // Delete gui and return to previous menu.
                this.delete();

                u.staffGui = new LocationRequests(provider);
                u.staffGui.open(u.player);
            });
        }

        // Return
        if (type == AddLocationType.ADD) {
            setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Open the explore" + " menu.")), (NetworkUser u) -> {

                // Delete this gui.
                this.delete();

                // Switch to the navigation menu.
                u.mainGui = new ExploreGui(provider, u);
                u.mainGui.open(u.player);
            });
        } else if (type == AddLocationType.REVIEW) {
            setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Return to " + "location requests.")), (NetworkUser u) -> {

                // Delete gui and return to previous menu.
                this.delete();

                u.staffGui = new LocationRequests(provider);
                u.staffGui.open(u.player);
            });
        } else {
            setItem(26, Utils.createItem(Material.SPRUCE_DOOR, 1, Utils.title("Return"), Utils.line("Return to staff " + "menu.")), (NetworkUser u) -> {

                // Delete gui and return to previous menu.
                this.delete();

                u.staffGui = new StaffGui(provider, u);
                u.staffGui.open(u.player);
            });
        }
    }

    private void addLocation(NetworkUser u) {

        // If the subcategory has been set, find the subcategory id.
        int subcategory_id = 0;
        if (!subcategory.equals("None")) {
            subcategory_id = globalSQL.getInt("SELECT id FROM location_subcategory WHERE name='" + subcategory + "';");
            if (subcategory_id == 0) {
                u.player.sendMessage(ChatUtils.error("The subcategory no longer exists, adding location without " + "subcategory."));
            }
        }

        if (subcategory_id == 0) {
            globalSQL.update("INSERT INTO location_data(location,category,coordinate) " + "VALUES('" + name + "','" + category + "'," + coordinate_id + ");");
        } else {
            globalSQL.update(
                    "INSERT INTO location_data(location,category,subcategory,coordinate) " + "VALUES('" + name + "'," + "'" + category + "'," + subcategory_id + "," + coordinate_id + ");");
        }

        u.player.sendMessage(ChatUtils.success("Location ").append(Component.text(name, NamedTextColor.DARK_AQUA)).append(ChatUtils.success(" added to exploration menu.")));

        // Delete gui.
        this.delete();
        u.mainGui = null;

        u.mainGui = new ExploreGui(provider, u);
        u.player.closeInventory();
    }

    public void updateLocation(NetworkUser u) {

        // If the subcategory has been set, find the subcategory id.
        int subcategory_id = 0;
        if (!subcategory.equals("None")) {
            subcategory_id = globalSQL.getInt("SELECT id FROM location_subcategory WHERE name='" + subcategory + "';");
            if (subcategory_id == 0) {
                u.player.sendMessage(ChatUtils.error("The subcategory no longer exists, adding location without " + "subcategory."));
            }
        }

        if (subcategory_id == 0) {
            globalSQL.update("UPDATE location_data SET location='" + name + "',category='" + category + "' WHERE " + "location='" + old_name + "';");
        } else {
            globalSQL.update(
                    "UPDATE location_data SET location='" + name + "',category='" + category + "'," + "subcategory=" + subcategory_id + " WHERE location='" + old_name + "';");
        }

        u.player.sendMessage(ChatUtils.success("Updated location ").append(Component.text(name, NamedTextColor.DARK_AQUA)));

        // Delete gui.
        this.delete();
        u.staffGui = null;

        u.player.closeInventory();
    }

    public void acceptRequest(NetworkUser u) {

        // Delete request.
        globalSQL.update("DELETE FROM location_requests WHERE location='" + old_name + "';");

        // If the subcategory has been set, find the subcategory id.
        int subcategory_id = 0;
        if (!subcategory.equals("None")) {
            subcategory_id = globalSQL.getInt("SELECT id FROM location_subcategory WHERE name='" + subcategory + "';");
            if (subcategory_id == 0) {
                u.player.sendMessage(ChatUtils.error("The subcategory no longer exists, adding location without " + "subcategory."));
            }
        }

        // Add location.
        if (subcategory_id == 0) {
            globalSQL.update("INSERT INTO location_data(location,category,coordinate) " + "VALUES('" + name + "','" + category + "'," + coordinate_id + ");");
        } else {
            globalSQL.update(
                    "INSERT INTO location_data(location,category,subcategory,coordinate) " + "VALUES('" + name + "'," + "'" + category + "'," + subcategory_id + "," + coordinate_id + ");");
        }

        // Notify player.
        u.player.sendMessage(ChatUtils.success("Accepted location request ").append(Component.text(name, NamedTextColor.DARK_AQUA)));
    }

    public void requestLocation(NetworkUser u) {

        // If the subcategory has been set, find the subcategory id.
        int subcategory_id = 0;
        if (!subcategory.equals("None")) {
            subcategory_id = globalSQL.getInt("SELECT id FROM location_subcategory WHERE name='" + subcategory + "';");
            if (subcategory_id == 0) {
                u.player.sendMessage(ChatUtils.error("The subcategory no longer exists, adding location without " + "subcategory."));
            }
        }

        if (subcategory_id == 0) {
            globalSQL.update("INSERT INTO location_requests(location,category,coordinate) " + "VALUES('" + name + "'," + "'" + category + "'," + coordinate_id + ");");
        } else {
            globalSQL.update(
                    "INSERT INTO location_requests(location,category,subcategory,coordinate) " + "VALUES('" + name + "','" + category + "'," + subcategory_id + "," + coordinate_id + ");");
        }

        // Notify reviewers.
        ChatMessage chatMessage = new ChatMessage(REVIEWER.getChannelName(), "server", ChatUtils.success("A new " + "location has been requested."));
        provider.chatAPI().sendChatMessage(chatMessage);

        u.player.sendMessage(ChatUtils.success("Location %s requested.", name));

        // Delete gui.
        this.delete();
        u.mainGui = null;

        u.mainGui = new ExploreGui(provider, u);
        u.player.closeInventory();
    }

    // Override the delete method to make sure selectSubcategory is also deleted.
    @Override
    public void delete() {
        super.delete();

        // If selectSubcategory exists, delete it.
        if (selectSubcategory != null) {
            selectSubcategory.delete();
            selectSubcategory = null;
        }
    }
}
