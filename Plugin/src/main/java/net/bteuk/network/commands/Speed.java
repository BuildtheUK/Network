package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.btuk.network.lib.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Speed extends AbstractCommand {

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NotNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        boolean isFly = player.isFlying();

        if (args.length < 1) {
            error(player);
            return;
        }

        float speed;

        try {
            speed = getMoveSpeed(args[0]);
        } catch (NumberFormatException e) {
            error(player);
            return;
        }

        speed = getRealMoveSpeed(speed, isFly);

        if (isFly) {
            player.setFlySpeed(speed);
            player.sendMessage(ChatUtils.success("Set flying speed to ")
                    .append(Component.text(args[0], NamedTextColor.DARK_AQUA)));
        } else {

            player.setWalkSpeed(speed);
            player.sendMessage(ChatUtils.success("Set walking speed to ")
                    .append(Component.text(args[0], NamedTextColor.DARK_AQUA)));
        }
    }

    // Returns the move speed from a string.
    private float getMoveSpeed(final String moveSpeed) throws NumberFormatException {
        float userSpeed;
        userSpeed = Float.parseFloat(moveSpeed);
        if (userSpeed > 10f) {
            userSpeed = 10f;
        } else if (userSpeed < 0.0001f) {
            userSpeed = 0.0001f;
        }
        return userSpeed;
    }

    // Converts the move speed to a Minecraft usable value.
    private float getRealMoveSpeed(final float userSpeed, final boolean isFly) {
        final float defaultSpeed = isFly ? 0.1f : 0.2f;
        float maxSpeed = 1f;

        if (userSpeed < 1f) {
            return defaultSpeed * userSpeed;
        } else {
            final float ratio = ((userSpeed - 1) / 9) * (maxSpeed - defaultSpeed);
            return ratio + defaultSpeed;
        }
    }

    // Error message.
    private void error(Player p) {
        p.sendMessage(ChatUtils.error("/speed [0-10]"));
    }

    @Override
    public String getLabel() {
        return "speed";
    }

    @Override
    public String getDescription() {
        return "Sets the players speed, value up to 10.";
    }
}
