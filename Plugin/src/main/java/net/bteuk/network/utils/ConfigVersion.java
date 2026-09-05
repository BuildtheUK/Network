package net.bteuk.network.utils;

import org.jspecify.annotations.NonNull;

/**
 * A record representing a configuration version with major, minor, and patch components.
 * Suffixes like -SNAPSHOT are ignored during parsing.
 */
public record ConfigVersion(int major, int minor, int patch) implements Comparable<ConfigVersion> {

    /**
     * Parses a version string into a ConfigVersion.
     * Only the first three numerical parts (major, minor, patch) are considered.
     * Anything after a hyphen (e.g. '-SNAPSHOT') is ignored.
     *
     * @param version the version string to parse
     * @return a ConfigVersion instance
     */
    public static ConfigVersion parse(String version) {
        if (version == null || version.isBlank()) {
            return new ConfigVersion(0, 0, 0);
        }

        // Exclude anything after the first hyphen (e.g., -SNAPSHOT)
        String cleanVersion = version.split("-")[0];
        String[] parts = cleanVersion.split("\\.");

        int major = parts.length > 0 ? tryParse(parts[0]) : 0;
        int minor = parts.length > 1 ? tryParse(parts[1]) : 0;
        int patch = parts.length > 2 ? tryParse(parts[2]) : 0;

        return new ConfigVersion(major, minor, patch);
    }

    private static int tryParse(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public int compareTo(ConfigVersion o) {
        if (this.major != o.major) {
            return Integer.compare(this.major, o.major);
        }
        if (this.minor != o.minor) {
            return Integer.compare(this.minor, o.minor);
        }
        return Integer.compare(this.patch, o.patch);
    }

    @Override
    public @NonNull String toString() {
        return major + "." + minor + "." + patch;
    }
}
