package net.bteuk.network;

import lombok.Getter;
import lombok.extern.java.Log;
import net.bteuk.network.api.CoordinateAPI;
import net.bteuk.network.api.NetworkAPI;
import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.api.WorldGuardAPI;
import net.bteuk.network.api.entity.ShutdownHook;
import net.bteuk.network.api.impl.CoordinateAPIImpl;
import net.bteuk.network.api.impl.PlotAPIImpl;
import net.bteuk.network.api.impl.TimerAPIImpl;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.commands.BuildingCompanionCommand;
import net.bteuk.network.commands.Buildings;
import net.bteuk.network.commands.Clear;
import net.bteuk.network.commands.Demote;
import net.bteuk.network.commands.Discord;
import net.bteuk.network.commands.Focus;
import net.bteuk.network.commands.Gamemode;
import net.bteuk.network.commands.Hat;
import net.bteuk.network.commands.Hdb;
import net.bteuk.network.commands.Help;
import net.bteuk.network.commands.Me;
import net.bteuk.network.commands.Msg;
import net.bteuk.network.commands.Navigator;
import net.bteuk.network.commands.Nick;
import net.bteuk.network.commands.Nightvision;
import net.bteuk.network.commands.Phead;
import net.bteuk.network.commands.Plot;
import net.bteuk.network.commands.Pmute;
import net.bteuk.network.commands.ProgressMap;
import net.bteuk.network.commands.Promote;
import net.bteuk.network.commands.Ptime;
import net.bteuk.network.commands.Punmute;
import net.bteuk.network.commands.Pweather;
import net.bteuk.network.commands.RegionCommand;
import net.bteuk.network.commands.Reply;
import net.bteuk.network.commands.Rules;
import net.bteuk.network.commands.Speed;
import net.bteuk.network.commands.Survey;
import net.bteuk.network.commands.TipsToggle;
import net.bteuk.network.commands.Where;
import net.bteuk.network.commands.Zone;
import net.bteuk.network.commands.give.GiveBarrier;
import net.bteuk.network.commands.give.GiveDebugStick;
import net.bteuk.network.commands.give.GiveLight;
import net.bteuk.network.commands.navigation.Back;
import net.bteuk.network.commands.navigation.Delhome;
import net.bteuk.network.commands.navigation.Home;
import net.bteuk.network.commands.navigation.Homes;
import net.bteuk.network.commands.navigation.Navigation;
import net.bteuk.network.commands.navigation.PreviousLocationTracker;
import net.bteuk.network.commands.navigation.Server;
import net.bteuk.network.commands.navigation.Sethome;
import net.bteuk.network.commands.navigation.Spawn;
import net.bteuk.network.commands.navigation.Teleport;
import net.bteuk.network.commands.navigation.TpAccept;
import net.bteuk.network.commands.navigation.TpDeny;
import net.bteuk.network.commands.navigation.TpToggle;
import net.bteuk.network.commands.navigation.Tpll;
import net.bteuk.network.commands.navigation.Warp;
import net.bteuk.network.commands.navigation.Warps;
import net.bteuk.network.commands.staff.Ban;
import net.bteuk.network.commands.staff.Kick;
import net.bteuk.network.commands.staff.Mute;
import net.bteuk.network.commands.staff.Staff;
import net.bteuk.network.commands.staff.Unban;
import net.bteuk.network.commands.staff.Unmute;
import net.bteuk.network.core.Constants;
import net.bteuk.network.core.ServerType;
import net.bteuk.network.core.Time;
import net.bteuk.network.eventing.events.EventManager;
import net.bteuk.network.eventing.events.InviteEvent;
import net.bteuk.network.eventing.events.KickEvent;
import net.bteuk.network.eventing.events.TeleportEvent;
import net.bteuk.network.eventing.listeners.ChatListener;
import net.bteuk.network.eventing.listeners.CommandPreProcess;
import net.bteuk.network.eventing.listeners.Connect;
import net.bteuk.network.eventing.listeners.NetworkMoveListener;
import net.bteuk.network.eventing.listeners.NetworkTeleportListener;
import net.bteuk.network.eventing.listeners.PlayerInteract;
import net.bteuk.network.eventing.listeners.PreJoinServer;
import net.bteuk.network.lobby.Lobby;
import net.bteuk.network.lobby.LobbyCommand;
import net.bteuk.network.logging.BukkitForwardingHandler;
import net.bteuk.network.proxy.NetworkChatHandler;
import net.bteuk.network.proxy.NetworkCoreServerManager;
import net.bteuk.network.proxy.NetworkPlayerManager;
import net.bteuk.network.proxy.NetworkScheduler;
import net.bteuk.network.proxy.NetworkTabManager;
import net.bteuk.network.regions.RegionEvent;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.sql.RegionSQL;
import net.bteuk.network.services.NetworkPromotionService;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.socket.NetworkSocketHandler;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;
import net.bteuk.network.utils.NetworkConfig;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Roles;
import net.bteuk.network.utils.SwitchServer;
import net.bteuk.network.utils.Tips;
import net.bteuk.network.utils.Utils;
import net.bteuk.network.utils.staff.Moderation;
import net.bteuk.network.utils.worldguard.WorldGuard;
import net.bteuk.teachingtutorials.services.PromotionService;
import net.buildtheearth.terraminusminus.TerraConfig;
import org.btuk.minecraft.gui.GuiListener;
import org.btuk.minecraft.gui.GuiManager;
import org.btuk.network.lib.dto.OnlineUser;
import org.btuk.network.lib.dto.OnlineUserAdd;
import org.btuk.network.lib.dto.OnlineUserRemove;
import org.btuk.network.lib.dto.OnlineUsersReply;
import org.btuk.network.lib.dto.ServerStartup;
import org.btuk.proxy.app.ProxyController;
import org.btuk.proxy.core.socket.ProxySocketHandler;
import org.btuk.proxy.database.DatabaseInit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import teachingtutorials.utils.DBConnection;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Log
public final class Network extends JavaPlugin implements NetworkAPI {

    // If the server can shut down.
    public boolean allowShutdown;

    @Getter
    private ItemStack navigatorItem;
    public RegionSQL regionSQL;
    // Movement listeners.
    public NetworkMoveListener moveListener;
    public NetworkTeleportListener teleportListener;
    // Return an this of the regionManager.
    // RegionManager
    @Getter
    private RegionManager regionManager;
    // List of users connected to the network.
    @Getter
    private Map<UUID, OnlineUser> onlineUsers;

    private Map<UUID, NetworkUser> networkUsers;
    // SQL
    private PlotSQL plotSQL;

    @Getter
    private GlobalSQL globalSQL;
    // Chat
    @Getter
    private CustomChat chat;
    // Timers
    @Getter
    private TimerAPIImpl timerAPI;

    // Tpll Command
    @Getter
    private Tpll tpll;

    // Tutorials DB connection
    private DBConnection tutorialsDBConnection;

    private PlotAPI plotAPI;

    private Constants constants;

    @Getter
    private ServerAPI serverAPI;

    @Getter
    private EventManager eventAPI;

    private final List<ShutdownHook> shutdownHooks = new ArrayList<>();

    @Getter
    private CoordinateAPI coordinateAPI;

    @Getter
    private Roles roleAPI;

    @Override
    public void onEnable() {

        // Setup the logger.
        Logger base = Logger.getLogger("net.bteuk.network");
        base.setLevel(Level.ALL);
        base.setUseParentHandlers(false);

        // Make sure we don't accumulate multiple handlers across reloads
        for (Handler h : base.getHandlers()) {
            base.removeHandler(h);
        }

        base.addHandler(new BukkitForwardingHandler(getLogger()));

        allowShutdown = true;

        // Sets the config if the file has not yet been created.
        ConfigurationSerialization.registerClass(ConfigurationSerializable.class);
        saveDefaultConfig();

        // Update the config to the latest version if it's outdated.
        // It will copy over any keys that remain the same.
        // This will also set the status variable to access the config project-wide.
        NetworkConfig networkConfig = new NetworkConfig(this);
        networkConfig.updateConfig();

        if (!networkConfig.getConfig().getBoolean("enabled")) {

            getLogger().warning("The config must be configured before the plugin can be enabled!");
            getLogger().warning("Please edit the database values in the config, give the server a unique name and " + "then set 'enabled: true'");
            getLogger().warning("Also make sure to set the server to the correct type.");
            return;
        }

        constants = networkConfig.getConstants();

        // Setup MySQL
        try {
            DatabaseInit init = new DatabaseInit();
            String host = networkConfig.getConfig().getString("host");
            int port = networkConfig.getConfig().getInt("port");
            String username = networkConfig.getConfig().getString("username");
            String password = networkConfig.getConfig().getString("password");

            // Global Database
            String globalDatabase = networkConfig.getConfig().getString("database.global");
            DataSource globalDataSource = init.mysqlSetup(globalDatabase, host, port, username, password);
            globalSQL = new GlobalSQL(globalDataSource, constants);

            // Region Database
            String regionDatabase = networkConfig.getConfig().getString("database.region");
            DataSource regionDataSource = init.mysqlSetup(regionDatabase, host, port, username, password);
            if (constants.regionsEnabled()) {
                regionSQL = new RegionSQL(regionDataSource);
            }

            // Plot Database
            String plotDatabase = networkConfig.getConfig().getString("database.plot");
            DataSource plotDataSource = init.mysqlSetup(plotDatabase, host, port, username, password);
            if (constants.plotSystemEnabled()) {
                plotSQL = new PlotSQL(plotDataSource);
            }
        } catch (SQLException | RuntimeException e) {
            getLogger().severe("Failed to connect to the database, please check that you have set the config values " + "correctly.");
            getLogger().severe("Disabling Network");
            return;
        }

        // Setup tutorials DB connection and connect
        if (constants.tutorials()) {
            // Initialise the DBConnection object
            tutorialsDBConnection = new DBConnection();

            // Extract database details from the config
            String szHost = networkConfig.getConfig().getString("tutorials.database.host");
            int iPort = networkConfig.getConfig().getInt("tutorials.database.port");
            String szDBName = networkConfig.getConfig().getString("tutorials.database.name");
            String szUsername = networkConfig.getConfig().getString("tutorials.database.username");
            String szPassword = networkConfig.getConfig().getString("tutorials.database.password");

            // Set up the DBConnection object with details
            tutorialsDBConnection.externalMySQLSetup(szHost, iPort, szDBName, szUsername, szPassword);

            // Attempt to connect to the DB
            if (!tutorialsDBConnection.connect()) {
                getLogger().severe("Failed to connect to the Tutorials database, please check that you have set the " + "config values correctly.");
                getLogger().severe("Disabling Network");
                return;
            }
        }

        if (!globalSQL.hasRow("SELECT name FROM server_data WHERE name='" + constants.serverName() + "';")) {

            // Add server to database and enable server.
            if (globalSQL.update("INSERT INTO server_data(name,type) VALUES('" + constants.serverName() + "','" + constants.serverType() + "');")) {

                // Enable plugin.
                getLogger().info("Server added to database with name " + constants.serverName() + " and type " + constants.serverType());
                getLogger().info("Enabling Plugin");
                enablePlugin();
            } else {

                // If the server is not in the database, shut down plugin.
                getLogger().severe("Failed to add server to database, disabling plugin!");
            }
        } else {

            // Enable plugin.
            getLogger().info("Enabling Plugin");
            enablePlugin();
        }
    }

    // Server enabling procedure when the config has been set up.
    public void enablePlugin() {

        // Create the user lists.
        networkUsers = new HashMap<UUID, NetworkUser>();
        onlineUsers = new HashMap<UUID, OnlineUser>();

        // Set up the message sender.
        MessageSender messageSender = new MessageSender(constants);

        // Set up the timer api
        timerAPI = new TimerAPIImpl(this);

        GuiManager networkGuiManager = new GuiManager();

        CommandManager commandManager = new CommandManager(this);

        PreviousLocationTracker previousLocationTracker = new PreviousLocationTracker(globalSQL);

        this.coordinateAPI = new CoordinateAPIImpl(globalSQL);
        this.eventAPI = new EventManager(globalSQL, constants, previousLocationTracker);
        WorldGuardAPI worldGuardAPI = new WorldGuard();

        roleAPI = new Roles(this, plotSQL, messageSender);

        serverAPI = new SwitchServer(this, constants, messageSender);

        if (constants.plotSystemEnabled()) {
            plotAPI = new PlotAPIImpl(plotSQL, globalSQL);
        }

        // Enable tab.
        TabManager tabManager = new TabManager(this, constants, roleAPI);

        Nightvision nightvision = new Nightvision(this);

        Moderation moderation = new Moderation(this, eventAPI, messageSender);

        // Enables chat, both global chat and normal chat are handled through it.
        chat = new CustomChat(this, messageSender, constants, roleAPI);

        Afk afk = new Afk(this, messageSender, chat);
        commandManager.registerCommand(afk);

        // Create the region manager if enabled.
        if (constants.regionsEnabled()) {
            regionManager = new RegionManager(regionSQL, this, coordinateAPI, eventAPI, worldGuardAPI, constants, this, serverAPI);
            commandManager.registerCommand(new RegionCommand(regionManager, eventAPI, constants));
        }

        // Setup connect, this handles all connections to the server.
        // Listener and manager of server connections.
        Connect connect = new Connect(this, constants, tabManager, roleAPI, networkGuiManager, nightvision, eventAPI, regionManager, messageSender);

        Teleport teleport = new Teleport(this, previousLocationTracker, eventAPI, serverAPI, constants, messageSender);
        commandManager.registerCommand(teleport);
        commandManager.registerCommand(new TpToggle(this));
        commandManager.registerCommand(new TpAccept(this, messageSender));
        commandManager.registerCommand(new TpDeny(this, messageSender));

        // Set up socket listening - used for sending messages cross-server on multi-server setups
        NetworkSocketHandler socketHandler = new NetworkSocketHandler(this, chat, tabManager, connect, constants, teleport);

        // If running in standalone mode, set up the proxy logic locally.
        if (constants.standalone()) {
            try {
                initStandaloneMode(socketHandler, messageSender);
            } catch (IOException e) {
                log.severe("Failed to initialise standalone mode, disabling plugin!");
                return;
            }
        }

        // Create the navigator.
        navigatorItem = Utils.createItem(Material.NETHER_STAR, 1, Utils.title("Navigator"), Utils.line("Click to open the navigator."));

        // Register events.
        new PreJoinServer(this, constants, moderation);

        new GuiListener(networkGuiManager).register(this);

        moveListener = new NetworkMoveListener(this, afk);
        teleportListener = new NetworkTeleportListener(this);

        // Set up the lobby, most features are only enabled in the lobby server.
        Lobby lobby = new Lobby(this, constants, serverAPI, eventAPI);

        // Enable commands
        if (constants.moderationEnabled()) {
            commandManager.registerCommand(new Kick(this, moderation));
            commandManager.registerCommand(new Mute(this, moderation));
            commandManager.registerCommand(new Unmute(this, moderation));
            commandManager.registerCommand(new Ban(this, moderation));
            commandManager.registerCommand(new Unban(this, moderation));
        }

        Back back = new Back(this, constants, eventAPI, serverAPI, previousLocationTracker);
        commandManager.registerCommand(back);

        if (constants.tpllEnabled()) {
            TerraConfig.reducedConsoleMessages = true;
            tpll = new Tpll(this, constants.tpllRequiresPermission(), regionManager, constants, plotSQL, eventAPI, serverAPI, back, globalSQL, previousLocationTracker);
            commandManager.registerCommand(tpll);
        }

        if (constants.ll()) {
            commandManager.registerCommand(new Where(plotSQL, plotAPI, constants));
        }

        if (!constants.standalone()) {
            commandManager.registerCommand(new LobbyCommand(lobby, constants));
            commandManager.registerCommand(new Spawn(constants, back, lobby, eventAPI, serverAPI, globalSQL, previousLocationTracker));
            commandManager.registerCommand(new Server(globalSQL, constants, serverAPI));
        }

        if (constants.homesEnabled()) {
            commandManager.registerCommand(new Sethome(this));
            commandManager.registerCommand(new Home(this, constants, eventAPI, serverAPI));
            commandManager.registerCommand(new Delhome(this));
            commandManager.registerCommand(new Homes(this));
        }

        if (constants.UKSurvey()) {
            commandManager.registerCommand(new Survey(this, globalSQL));
        }

        /*
         * Utility commands.
         */
        commandManager.registerCommand(new Buildings(this, plotSQL, constants));
        commandManager.registerCommand(new Discord(this, roleAPI, constants, messageSender));
        commandManager.registerCommand(new Focus(this, messageSender));

        commandManager.registerCommand(nightvision);
        commandManager.registerCommand(new Speed());
        commandManager.registerCommand(new Help(constants, roleAPI));
        commandManager.registerCommand(new Rules(lobby));
        commandManager.registerCommand(new Clear());
        commandManager.registerCommand(new GiveDebugStick(this));
        commandManager.registerCommand(new GiveLight(this));
        commandManager.registerCommand(new GiveBarrier(this));
        commandManager.registerCommand(new Gamemode(constants));
        commandManager.registerCommand(new Phead(this, globalSQL));
        if (constants.skullsEnabled()) {
            commandManager.registerCommand(new Hdb());
        }
        if (constants.progressMap()) {
            commandManager.registerCommand(new ProgressMap(constants));
        }
        if (constants.tips()) {
            commandManager.registerCommand(new TipsToggle(this));
        }
        commandManager.registerCommand(new Ptime());
        commandManager.registerCommand(new Pweather());
        // commands.register("season", "Command for creating, starting and ending seasons.", List.of("seasons"), new Season());
        // commands.register("exp", "Test command for adding exp.", new Exp());
        commandManager.registerCommand(new BuildingCompanionCommand(this, constants, regionManager));

        commandManager.registerCommand(new Pmute(this, messageSender));
        commandManager.registerCommand(new Punmute(this, messageSender));

        commandManager.registerCommand(Msg.of(this, "msg", messageSender));
        commandManager.registerCommand(Msg.of(this, "w", messageSender));
        commandManager.registerCommand(Msg.of(this, "tell", messageSender));

        commandManager.registerCommand(new Reply(messageSender));
        commandManager.registerCommand(new Nick(this, messageSender));
        commandManager.registerCommand(new Hat());

        commandManager.registerCommand(new Promote(this, roleAPI, chat));
        commandManager.registerCommand(new Demote(this, roleAPI, chat));

        commandManager.registerCommand(new Me());

        Navigator navigator = new Navigator(this, networkGuiManager, constants, globalSQL, regionSQL, regionManager, plotSQL, plotAPI, lobby, back, eventAPI, serverAPI,
                nightvision, roleAPI, tutorialsDBConnection, chat, moderation, previousLocationTracker);
        commandManager.registerCommand(navigator);
        new PlayerInteract(this, navigator);

        if (constants.warpsEnabled()) {
            commandManager.registerCommand(new Warp(this, constants, plotAPI, back, eventAPI, serverAPI, previousLocationTracker));
            commandManager.registerCommand(new Warps(this));
            commandManager.registerCommand(new Navigation(this, navigator.getProvider()));
        }

        if (constants.plotSystemEnabled()) {
            commandManager.registerCommand(new Plot(navigator.getProvider()));
            commandManager.registerCommand(new Zone(plotSQL, eventAPI));
        }

        commandManager.registerCommand(new Staff(navigator.getProvider()));

        // Register the command pre-process to make sure network versions of commands run and not that of another plugin.
        new CommandPreProcess(this, constants, afk, connect, serverAPI, messageSender);

        // Create the rules-book.
        lobby.setGuiProvider(navigator.getProvider());
        lobby.loadRules();
        if (!constants.standalone()) {
            if (constants.serverType() == ServerType.LOBBY) {

                // Set spawn-location and enable auto-spawn teleport when falling in the void.
                lobby.setSpawn();
                lobby.enableVoidTeleport();

                lobby.reloadPortals();

                // Set the rules-lectern.
                lobby.setLectern();
            }

            // Set up the map.
            lobby.reloadMap(commandManager);
        }

        commandManager.enableCommands();

        // Enable tips.
        if (constants.tips()) {
            // Enable tips in chat.
            new Tips(this, constants);
        }

        // Create a default season if not exists.
        if (!globalSQL.hasRow("SELECT id FROM seasons WHERE id='default';")) {
            globalSQL.update("INSERT INTO seasons(id,active) VALUES('default',1);");
        }

        // Register Promotion Service for tutorials.
        if (constants.tutorials()) {
            try {
                Class.forName("net.bteuk.teachingtutorials.services.PromotionService");
                PromotionService promotionService = new NetworkPromotionService(roleAPI, chat);
                this.getServer().getServicesManager().register(PromotionService.class, promotionService, this, ServicePriority.High);
                log.info("Registered Network Promotion Service");
            } catch (ClassNotFoundException e) {
                // Only load the PromotionService if the class exists.
            }
        }

        // Register all the events.
        eventAPI.registerEvent("invite", new InviteEvent(globalSQL, plotAPI, regionManager));
        eventAPI.registerEvent("teleport", new TeleportEvent(globalSQL, plotAPI, regionManager, constants, serverAPI, eventAPI, tpll, lobby));
        if (constants.regionsEnabled()) {
            eventAPI.registerEvent("region", new RegionEvent(regionManager, chat, globalSQL, coordinateAPI));
        }
        eventAPI.registerEvent("kick", new KickEvent());

        // Start the Network timers.
        new Timers(this, globalSQL, eventAPI, constants, afk);

        // Register the chat listener.
        new ChatListener(this, moderation, afk, messageSender);

        // Let the Proxy know that the server is enabled.
        messageSender.sendSocketMessage(new ServerStartup(constants.serverName()));

        // Register the API as a service.
        getServer().getServicesManager().register(NetworkAPI.class, this, this, ServicePriority.Normal);

    }

    private void initStandaloneMode(NetworkSocketHandler socketHandler, MessageSender messageSender) throws IOException {
        log.info("Loading Network in standalone mode.");
        ProxyController proxyController = new ProxyController(getDataFolder());

        NetworkScheduler scheduler = new NetworkScheduler(this);
        NetworkPlayerManager playerManager = new NetworkPlayerManager(this);
        NetworkCoreServerManager serverManager = new NetworkCoreServerManager(this);

        NetworkChatHandler chatHandler = new NetworkChatHandler(socketHandler);
        NetworkTabManager standaloneTabManager = new NetworkTabManager(getServer(), roleAPI, constants, proxyController.getConfig(), proxyController.getCoreUserManager(), chatHandler, scheduler);

        // Set up the local socket handler.
        Consumer<ProxySocketHandler> socketInitializer = messageSender.setupStandaloneOutputSocket();

        proxyController.start(chatHandler, scheduler, serverManager, playerManager, standaloneTabManager, socketInitializer);
    }

    @Override
    public void onDisable() {

        shutdownHooks.forEach(ShutdownHook::shutdown);

        // Shut down chat.
        if (chat != null) {
            chat.onDisable();
        }

        for (NetworkUser u : getUsers()) {
            String uuid = u.player.getUniqueId().toString();

            // Remove any outstanding invites that this player has sent.
            plotSQL.update("DELETE FROM plot_invites WHERE owner='" + uuid + "';");
            plotSQL.update("DELETE FROM zone_invites WHERE owner='" + uuid + "';");

            // Remove any outstanding invites that this player has received.
            plotSQL.update("DELETE FROM plot_invites WHERE uuid='" + uuid + "';");
            plotSQL.update("DELETE FROM zone_invites WHERE uuid='" + uuid + "';");

            // Set last_online time in playerdata.
            globalSQL.update("UPDATE player_data SET last_online=" + Time.currentTime() + " WHERE " + "UUID='" + uuid + "';");

            // Reset last logged time.
            if (u.isAfk()) {
                u.last_movement = Time.currentTime();
                u.setAfk(false);
            }
        }

        // Disconnect from tutorials
        if (constants.tutorials() && tutorialsDBConnection != null) {
            tutorialsDBConnection.disconnect();
        }
    }

    public @Nullable NetworkUser getUser(Player p) {
        return networkUsers.get(p.getUniqueId());
    }

    public Optional<NetworkUser> getNetworkUserByUuid(String uuid) {
        return Optional.ofNullable(networkUsers.get(UUID.fromString(uuid)));
    }

    public Collection<NetworkUser> getUsers() {
        return networkUsers.values();
    }

    public void addUser(NetworkUser u) {
        networkUsers.put(u.player.getUniqueId(), u);
    }

    public void removeUser(NetworkUser u) {
        networkUsers.remove(u.player.getUniqueId());
    }

    public void handleOnlineUsersReply(OnlineUsersReply onlineUsersReply) {
        onlineUsers.putAll(onlineUsersReply.getOnlineUsers().stream().collect(Collectors.toMap(onlineUser -> UUID.fromString(onlineUser.getUuid()), onlineUser -> onlineUser)));
    }

    public void handleOnlineUserAdd(OnlineUserAdd onlineUserAdd) {
        onlineUsers.put(UUID.fromString(onlineUserAdd.getUser().getUuid()), onlineUserAdd.getUser());
    }

    public void handleOnlineUserRemove(OnlineUserRemove onlineUserRemove) {
        onlineUsers.remove(UUID.fromString(onlineUserRemove.getUuid()));
    }

    public boolean isOnlineOnNetwork(String uuid) {
        return onlineUsers.containsKey(UUID.fromString(uuid));
    }

    public Optional<OnlineUser> getOnlineUserByUuid(String uuid) {
        return Optional.ofNullable(onlineUsers.get(UUID.fromString(uuid)));
    }

    public Optional<OnlineUser> getOnlineUserByNameIgnoreCase(String name) {
        return onlineUsers.values().stream().filter(onlineUser -> onlineUser.getName().equalsIgnoreCase(name)).findFirst();
    }

    public PlotAPI getPlotAPI() {
        if (plotAPI == null) {
            throw new IllegalStateException("The plot system is not enabled");
        }
        return plotAPI;
    }

    public boolean isTutorialsEnabled() {
        return constants.tutorials();
    }

    public boolean isStandalone() {
        return constants.standalone();
    }

    public int getMinY() {
        return constants.minY();
    }

    public int getMaxY() {
        return constants.maxY();
    }

    @Override
    public void registerShutdownHook(ShutdownHook hook) {
        shutdownHooks.add(hook);
    }
}
