package net.bteuk.network.utils;

import lombok.extern.java.Log;
import net.bteuk.network.Network;
import net.bteuk.network.core.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class that manages the automated tips in chat.
 * The frequency is specified in the config.
 * Each builder role can have a file, if no file exists for the role, no message is sent.
 */
@Log
public class Tips {

    private Map<String, TipsList> tipsMap;

    /**
     * Load the tips from the text files in the tips folder, if any exist.
     * If no files exist in the directory don't load tips.
     */
    public Tips(Network instance, Constants constants) {

        // Create the directory if not exists.
        File file = new File(instance.getDataFolder() + "/tips");

        if (!file.exists()) {

            if (file.mkdir()) {

                // Add an example file.
                try {

                    FileUtils.copyToFile(Objects.requireNonNull(instance.getResource("tips-example.txt")),
                            new File(file + "/tips-example.txt"));
                    log.info("Created tips directory and added example file.");
                } catch (IOException | NullPointerException e) {
                    log.severe("An error occurred while creating the tips directory and example file: " + e.getLocalizedMessage());
                }
            }
        } else {

            // The directory exists, therefore load all txt files.
            File[] files = file.listFiles();

            if (files != null) {

                tipsMap = new HashMap<>();

                for (File txtFile : files) {

                    try {
                        List<String> lines = Files.readAllLines(Path.of(txtFile.getAbsolutePath()));

                        // Trim the list of whitespace lines.
                        lines = lines.stream().filter(str -> !str.trim().isEmpty()).collect(Collectors.toList());

                        // The file must contain at least 1 line.
                        if (!lines.isEmpty()) {
                            Collections.shuffle(lines);
                            tipsMap.put(txtFile.getName().replace(".txt", ""), new TipsList(lines));
                        }
                    } catch (IOException e) {
                        log.severe("An error occurred while loading the tips file " + txtFile.getName() + ": " + e.getLocalizedMessage());
                    }
                }

                // If the tipsMap is not empty start the tips timer.
                if (!tipsMap.isEmpty()) {

                    // Get interval.
                    long frequency = constants.tipsFrequency() * 60L * 1000L;

                    instance.getTimerAPI().registerTimer(() -> {

                        // For all online players see if their builder role has tips, if true send them the current tip.
                        for (NetworkUser user : instance.getUsers()) {

                            // Check if the user has tips enabled.
                            if (user.isTipsEnabled()) {

                                // Get builder role from database.
                                String role = instance.getGlobalSQL().getString("SELECT builder_role " +
                                        "FROM player_data WHERE uuid='" + user.player.getUniqueId() + "';");

                                if (tipsMap.containsKey(role)) {
                                    user.player.sendMessage(Utils.tip(tipsMap.get(role).getTip()));
                                }
                            }
                        }

                        // Increment the counter on all TipsLists
                        tipsMap.values().forEach(TipsList::increment);
                    }, frequency, 60000L);

                    log.info("Enabled tips timer!");
                }
            }
        }
    }

    /**
     * A list of tips with functionality to get the next tip.
     */
    private static class TipsList {

        private final String[] tips;
        private int counter;

        /**
         * Creates a new TipsList using a List of tips.
         *
         * @param tipsList list of tips
         */
        public TipsList(List<String> tipsList) {

            tips = tipsList.toArray(new String[0]);
            counter = 0;
        }

        /**
         * Increase the index counter, if at the maximum value set to 0.
         */
        public void increment() {

            if (counter >= (tips.length - 1)) {
                counter = 0;
            } else {
                counter++;
            }
        }

        /**
         * Get the tip at the current index.
         *
         * @return tip at the index counter
         */
        public Component getTip() {
            MiniMessage miniMessage = MiniMessage.miniMessage();
            return miniMessage.deserialize(tips[counter]);
        }
    }
}
