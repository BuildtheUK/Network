package net.bteuk.network.commands.Navigator;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.AbstractCommand;
import net.bteuk.network.gui.tutorials.TutorialsGui;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Tutorials extends AbstractCommand {

    public Tutorials (Navigator nav){
        this.nav = nav;
    }
    Navigator nav;

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {
        nav.execute(stack,new String[] {"tutorials"});
    }

    @Override
    public String getLabel() {
        return "tutorial";
    }

    @Override
    public String getDescription() {
        return "Opens the tutorials gui";
    }

    @Override
    public List<String> getAliases() {return List.of("apply");}
}
