package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.bteuk.network.commands.tabcompleters.FixedArgSelector;
import net.bteuk.network.socket.MessageSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.btuk.network.lib.dto.ProgressMapVerification;
import org.btuk.network.lib.utils.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ProgressMap extends AbstractCommand{

    private final String baseLink;
    private final MessageSender messageSender;
    public ProgressMap(String link,MessageSender messageSender){
        this.messageSender = messageSender;
        setTabCompleter(new FixedArgSelector(List.of("link","verify"),0));
        this.baseLink = link;
    }

    @Override
    public String getLabel() {
        return "progressmap";
    }

    @Override
    public String getDescription() {
        return "Commands for our progress map";
    }

    @Override
    public List<String> getAliases() {
        return List.of("progress");
    }

    @Override
    public void execute(@NonNull CommandSourceStack stack, String @NonNull [] args) {
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }
        if (args.length == 0){
            sendClickableLink(stack.getSender(),baseLink,"Click to view our progress map.");
            return;
        }
        //not an if as I may want more options later
        switch (args[0]){
            case "verify":
                //sends a verification request to proxy
                ProgressMapVerification message = new ProgressMapVerification(player.getUniqueId().toString());
                messageSender.sendSocketMessage(message);
                player.sendMessage(ChatUtils.success("Account verification request sent"));
                break;
            default:
                sendClickableLink(stack.getSender(),baseLink,"Click to view our progress map.");
        }


    }

    private void sendClickableLink(CommandSender sender,String link,String message) {
        if (link == null) {
            return;
        }

        Component chatLink = ChatUtils.success(message);
        chatLink = chatLink.clickEvent(ClickEvent.openUrl(link));
        sender.sendMessage(chatLink);
    }
}
