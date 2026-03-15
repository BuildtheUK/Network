package net.bteuk.network.utils;

public class PlotValues {

    // Returns the plot difficulty name.
    public static String difficultyName(int difficulty) {

        return switch (difficulty) {
            case 1 -> "Easy";
            case 2 -> "Normal";
            case 3 -> "Hard";
            default -> null;
        };
    }

    // Returns the plot size name.
    public static String sizeName(int size) {

        return switch (size) {
            case 1 -> "Small";
            case 2 -> "Medium";
            case 3 -> "Large";
            default -> null;
        };
    }
}
