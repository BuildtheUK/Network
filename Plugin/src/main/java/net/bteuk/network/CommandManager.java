package net.bteuk.network;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.bteuk.network.api.EventAPI;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.commands.Afk;
import net.bteuk.network.core.Constants;
import net.bteuk.network.regions.RegionManager;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class CommandManager {

    private final Network instance;

    private final Set<AbstractCommand> commandsToRegister = new HashSet<>();

    public CommandManager(Network instance) {
        this.instance = instance;
    }

    public void registerCommand(AbstractCommand command) {
        commandsToRegister.add(command);
    }

    public void enableCommands() {
        LifecycleEventManager<@NotNull Plugin> manager = instance.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();

            commandsToRegister.forEach(command -> commands.register(command.getLabel(), command.getDescription(), command.getAliases(), command));
        });
    }

    public static void registerCommands(Network instance, Constants constants, EventAPI eventAPI, Afk afk, RegionManager regionManager) {

        LifecycleEventManager<Plugin> manager = instance.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();


        });
    }
}
