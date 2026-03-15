package net.bteuk.network.core.math;

import java.util.List;

public class Point {

    /**
     * Get the average point from a list of 2d points.
     *
     * @param points list of points
     * @return the average point
     */
    public static double[] getAveragePoint(List<double[]> points) {

        double size = points.size();
        double x = 0;
        double z = 0;

        for (double[] point : points) {
            x += point[0] / size;
            z += point[1] / size;
        }

        return new double[]{x, z};
    }

    /**
     * Get the distance between point 1 and point 2.
     *
     * @param p1 point 1
     * @param p2 point 2
     * @return the distance between the points
     */
    public static double distanceBetween(int[] p1, int[] p2) {
        return Math.sqrt(((p2[0] - p1[0]) * (p2[0] - p1[0])) + ((p2[1] - p1[1]) * (p2[1] - p1[1])));
    }

    public static double distanceBetween(double[] p1, double[] p2) {
        return Math.sqrt(((p2[0] - p1[0]) * (p2[0] - p1[0])) + ((p2[1] - p1[1]) * (p2[1] - p1[1])));
    }
}
