package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

public class Hdb extends AbstractCommand {

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
    }

    @Override
    public String getLabel() {
        return "hdb";
    }

    @Override
    public String getDescription() {
        return "Added so it can be routed to /skulls";
    }
}
