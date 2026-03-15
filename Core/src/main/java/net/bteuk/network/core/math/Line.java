package net.bteuk.network.core.math;

import lombok.Getter;
import lombok.Setter;

/**
 * Representation of a line.
 */
@Getter
public class Line {

    private final double a;
    @Setter
    private double b;

    public Line(double[] pointA, double[] pointB) {
        // Calculate the slope A and B, the starting y at x=0.
        a = (pointB[1] - pointA[1]) / (pointB[0] - pointA[0]);
        b = pointA[1] - (pointA[0] * a);
    }

    public Line(double a, double b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Get the intersection point between 2 lines, or null if no intersect exists.
     *
     * @param lineA line A
     * @param lineB line B
     * @return the point of intersect, if exists.
     */
    public static double[] getIntersect(Line lineA, Line lineB) {
        // If the angle of both lines is equal, then there is no intersect.
        if (lineA.a == lineB.a) {
            return null;
        }
        // Get the x coordinate.
        double x = (lineB.b - lineA.b) / (lineA.a - lineB.a);
        // Find the y coordinate.
        double y = (lineA.a * x) + lineA.b;
        // Return the coordinate as a double[].
        return new double[]{x, y};
    }

    public Line copyWithOffsetB(double offsetB) {
        return new Line(a, b + offsetB);
    }

    @Override
    public String toString() {
        return "[Line] a=" + a + ", b=" + b;
    }
}
