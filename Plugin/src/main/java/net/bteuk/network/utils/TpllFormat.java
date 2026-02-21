package net.bteuk.network.utils;

import lombok.Getter;
import lombok.Setter;
import net.buildtheearth.terraminusminus.util.geo.LatLng;

/**
 * Tpll format class, stores the altitude and coordinates gathered from the command arguments.
 */
@Getter
@Setter
public class TpllFormat {

    private double altitude = Double.NaN;
    private LatLng coordinates;
}
