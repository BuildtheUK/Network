package net.bteuk.network.commands.navigation;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.Network;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.api.ServerAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.tabcompleters.HomeSelector;
import net.bteuk.network.core.Constants;
import net.bteuk.network.papercore.LocationAdapter;
import net.bteuk.network.papercore.PlayerAdapter;
import net.bteuk.network.sql.GlobalSQL;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public class Home extends AbstractCommand {

    private final GlobalSQL globalSQL;
    private final Constants constants;
    private final EventAPI eventAPI;
    private final ServerAPI serverAPI;

    // Constructor to enable the command.
    public Home(Network instance, Constants constants, EventAPI eventAPI, ServerAPI serverAPI) {
        this.globalSQL = instance.getGlobalSQL();
        this.constants = constants;
        this.eventAPI = eventAPI;
        this.serverAPI = serverAPI;

        // Set tab completer.
        setTabCompleter(new HomeSelector(globalSQL));
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        // If no args teleport to default home, if exists.
        // Else try to set homes with specific names.
        // For multiple homes the player needs permission.
        int coordinateId;
        String message;
        if (args.length == 0) {

            // If a default home is set, teleport to it.
            if (!globalSQL.hasRow("SELECT uuid FROM home WHERE uuid='" + player.getUniqueId() + "' AND name IS NULL;")) {
                player.sendMessage(ChatUtils.error("You do not have a default home set, you can set it typing ").append(Component.text("/sethome", NamedTextColor.DARK_RED)));
                return;
            }

            // Get coordinate ID.
            coordinateId = globalSQL.getInt("SELECT coordinate_id FROM home WHERE uuid='" + player.getUniqueId() + "' AND name IS NULL;");
            message = "&aTeleported to your default home.";
        } else {

            // Check for permission.
            if (!player.hasPermission("uknet.navigation.homes")) {
                player.sendMessage(ChatUtils.error("You do not have permission to set multiple homes, you can only " + "use your default home with ")
                        .append(Component.text("/home", NamedTextColor.DARK_RED)));
                return;
            }

            // Check if a home with this name already exists.
            String name = String.join(" ", Arrays.copyOfRange(args, 0, args.length));

            // Check if home with this name exists.
            if (!globalSQL.hasRow("SELECT uuid FROM home WHERE uuid='" + player.getUniqueId() + "' AND name='" + name + "';")) {
                player.sendMessage(ChatUtils.error("You do not have a home with the name ").append(Component.text(name, NamedTextColor.DARK_RED)));
                return;
            }

            // Get coordinate ID.
            coordinateId = globalSQL.getInt("SELECT coordinate_id FROM home WHERE uuid='" + player.getUniqueId() + "' AND name='" + name + "';");
            message = "&aTeleported to your home &3" + name + "&a.";
        }

        // Get server.
        String server = globalSQL.getString("SELECT server FROM coordinates WHERE id=" + coordinateId + ";");

        // Check if server is current.
        if (Objects.equals(constants.serverName(), server)) {

            // Get default home location from the coordinate id.
            Location l = globalSQL.getLocation(coordinateId);

            // Teleport to the location.
            player.teleport(l);
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        } else if (!constants.standalone()) {

            // Switch server with join event.
            eventAPI.createTeleportEvent(true, player.getUniqueId().toString(), "teleport coordinateID " + coordinateId, "&aTeleported to your default home.",
                    LocationAdapter.adapt(player.getLocation()));

            // Switch server.
            serverAPI.switchServer(PlayerAdapter.adapt(player), server);
        }
    }

    @Override
    public String getLabel() {
        return "home";
    }

    @Override
    public String getDescription() {
        return "Teleport to your home.";
    }
}
