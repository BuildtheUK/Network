package net.bteuk.network.utils;

import net.bteuk.network.gui.NetworkRefreshableGui;
import net.bteuk.network.lib.utils.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Random;

public class LightsOut extends NetworkRefreshableGui {

    // Lights out is a simple game where the objective is to turn off all the lights.
    // You start with a 6*9 grid of lights that are either on or off.
    // Click on any slot will invert the value of the slot and all adjacent slots (not diagonals).

    private final NetworkUser u;
    private final long startTime;
    private boolean[][] game;

    public LightsOut(NetworkUser u) {

        super(54, Component.text("Lights Out", NamedTextColor.AQUA, TextDecoration.BOLD));

        this.u = u;
        startTime = Time.currentTime();

        initialize();
        setItems();
    }

    public void initialize() {

        // Create a random layout of 0 and 1s.
        Random random = new Random();

        game = new boolean[9][6];

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 6; j++) {

                game[i][j] = random.nextBoolean();
            }
        }
    }

    public void endGame() {

        // Get time difference between start and end time.
        long timeDiff = Time.currentTime() - startTime;

        // Other game end functionality.
        u.player.closeInventory();
        u.lightsOut = null;
        this.delete();

        u.player.sendMessage(ChatUtils.success("Congratulations, you beat Lights Out!"));
        u.player.sendMessage(ChatUtils.success("You took ")
                .append(Component.text(Time.minutes(timeDiff), NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" " + Time.minuteString(timeDiff) + " and "))
                .append(Component.text(Time.seconds(timeDiff), NamedTextColor.DARK_AQUA))
                .append(ChatUtils.success(" " + Time.secondString(timeDiff) + ".")));
    }

    public void setItems() {

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 6; j++) {

                int finalI = i;
                int finalJ = j;

                if (game[i][j]) {

                    // Light is on.
                    setItem(i + j * 9, Utils.createItem(Material.SEA_LANTERN, 1, Component.empty()),
                            u -> invertLights(finalI, finalJ));
                } else {

                    // Light is off.
                    setItem(i + j * 9, Utils.createItem(Material.REDSTONE_LAMP, 1, Component.empty()),
                            u -> invertLights(finalI, finalJ));
                }
            }
        }
    }

    public void invertLights(int i, int j) {

        // Invert this slot and the adjacent slots.
        game[i][j] = !game[i][j];

        if (i != 0) {
            game[i - 1][j] = !game[i - 1][j];
        }

        if (i != 8) {
            game[i + 1][j] = !game[i + 1][j];
        }

        if (j != 0) {
            game[i][j - 1] = !game[i][j - 1];
        }

        if (j != 5) {
            game[i][j + 1] = !game[i][j + 1];
        }

        // Refresh the gui.
        refresh();
    }

    public void refresh() {

        // Check if solution is complete.
        // If it is then stop end the game.
        if (Arrays.deepEquals(game, new boolean[9][6])) {

            endGame();
        } else {

            // Change items.
            clearGui();
            setItems();

            // Set contents of inventory.
            u.player.getOpenInventory().getTopInventory().setContents(getInventory().getContents());
        }
    }
}

