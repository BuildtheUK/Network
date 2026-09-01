package net.bteuk.network.building_companion;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.commands.navigation.Tpll;
import net.bteuk.network.core.Constants;
import net.bteuk.network.regions.RegionManager;
import net.bteuk.network.regions.RegionUser;
import net.bteuk.network.utils.TpllFormat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Optional;

@Log
public class TpllListener implements Listener {

    private final BuildingCompanion companion;

    private final Constants constants;

    private final RegionManager regionManager;

    public TpllListener(BuildingCompanion companion, Network instance, Constants constants, RegionManager regionManager) {
        this.companion = companion;
        this.constants = constants;
        this.regionManager = regionManager;
        Bukkit.getServer().getPluginManager().registerEvents(this, instance);
    }

    @EventHandler
    public void onCommandPreProcess(PlayerCommandPreprocessEvent e) {
        if (companion.playerNotEquals(e.getPlayer())) {
            return;
        }

        log.info(e.getMessage());

        if (e.getMessage().startsWith("/network:tpll")) {

            String[] command = e.getMessage().split(" ");

            if (command.length == 1) {
                // Command has no arguments, return.
                return;
            }

            // Convert the command to a usage format.
            TpllFormat format = Tpll.getUsableTpllFormat(Arrays.copyOfRange(command, 1, command.length));

            double[] proj;

            try {
                proj = Tpll.BTE_GENERATOR_SETTINGS.projection().fromGeo(format.getCoordinates().getLng(),
                        format.getCoordinates().getLat());
            } catch (Exception ex) {
                // No coordinates were parsed, return.
                return;
            }

            // Apply coordinate transform if regions are enabled.
            Location l = new Location(e.getPlayer().getWorld(), proj[0], 1, proj[1]);
            if (constants.regionsEnabled()) {
                Optional<RegionUser> optionalRegionUser = regionManager.getUserByPlayer(e.getPlayer());
                if (optionalRegionUser.isPresent()) {
                    RegionUser regionUser = optionalRegionUser.get();
                    Location newLocation = l.clone();
                    newLocation.setX(l.getX() + regionUser.getDeltaX());
                    newLocation.setZ(l.getZ() + regionUser.getDeltaZ());
                    l = newLocation;
                }
            }

            // Add a new corner, or update an existing one.
            companion.addLocation(l);
        }
    }
}
