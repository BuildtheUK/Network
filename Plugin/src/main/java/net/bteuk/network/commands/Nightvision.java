package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.Network;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Log
public class Nightvision extends AbstractCommand {

    private final Network instance;

    public Nightvision(Network instance) {
        this.instance = instance;
    }

    public void toggleNightvision(NetworkUser user) {
        if (user.isNightvisionEnabled()) {
            removeNightvision(user.player);
            user.setNightvisionEnabled(false);
            user.player.sendMessage(ChatUtils.success("Disabled nightvision."));
        } else {
            giveNightvision(user.player);
            user.setNightvisionEnabled(true);
            user.player.sendMessage(ChatUtils.success("Enabled nightvision."));
        }
    }

    public void giveNightvision(Player player) {
        // Remove any existing night vision first.
        Bukkit.getScheduler().runTask(instance, () -> {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false));
        });
    }

    public void removeNightvision(Player player) {
        Bukkit.getScheduler().runTask(instance, () -> {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        });
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        NetworkUser user = instance.getUser(player);

        // If u is null, cancel.
        if (user == null) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        toggleNightvision(user);
    }

    @Override
    public String getLabel() {
        return "nightvision";
    }

    @Override
    public String getDescription() {
        return "Toggle nightvision.";
    }

    @Override
    public List<String> getAliases() {
        return List.of("nv");
    }
}
