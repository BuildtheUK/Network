package net.bteuk.network.regions;

import java.util.Objects;

public record Region(String regionName, int x, int z) {

    public boolean equals(int x, int z) {
        return this.x == x && this.z == z;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (o instanceof String str) {
            return str.equals(regionName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
}