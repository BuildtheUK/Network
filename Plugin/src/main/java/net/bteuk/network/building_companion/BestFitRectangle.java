package net.bteuk.network.building_companion;

import lombok.Getter;
import net.bteuk.network.core.math.Line;
import net.bteuk.network.core.math.Point;

import java.util.Arrays;

/**
 * Utility class to find the best fit rectangle, given an input of 4 points.
 */
public class BestFitRectangle {

    private static final double ALPHA = 0.05;
    private static final double ADDITIVE = 0.01;
    private final double[][] input;
    @Getter
    private double[][] output;

    public BestFitRectangle(double[][] input) {
        this.input = input;
    }

    public boolean findBestFitRectangleCorners() {

        // Make sure no input has a duplicate value.
        // If a value is duplicate add a minor positive value to it.
        fixDuplicateValues();

        // Define point A, B, C and D. Where D is the diagonal corner from A.
        // D is the only corner from which point B and C are nearer than A.
        // If no such point exists, the inputs are unusable.
        // By default, we take pointA as the first input value.
        int pointDIndex = getPointDIndex();
        if (pointDIndex == 0) {
            return false;
        }
        double[][] sortedPoints = getSortedPoints(pointDIndex);
        Line[] initialLines = getInitialLines(sortedPoints);

        // Create right-angled lines.
        // This implies, line A and B are at the same angle.
        // Line C and D are at the same angle.
        // That the lines together form a rectangle (90-degree corners between the lines).
        Line[] lines = getRightAngledLines(initialLines);

        // Use an iterative process to find the best placement of the line.
        // This will be done by cycling through the lines and adjusting the b-value marginally.
        // We do this until the distance of the corner does not decrease the summed distance
        // for both the corners of the line to the initial corners.
        improveLinePlacement(lines, sortedPoints);

        // Set the output corners.
        output = new double[][]{
                Line.getIntersect(lines[0], lines[2]),
                Line.getIntersect(lines[0], lines[3]),
                Line.getIntersect(lines[1], lines[2]),
                Line.getIntersect(lines[1], lines[3])
        };

        return true;
    }

    // Add small positive values to duplicate inputs until no duplicates exist.
    private void fixDuplicateValues() {
        boolean redo;
        do {
            redo = false;
            for (int i = 0; i < input.length; i++) {
                for (int j = 0; j < input.length; j++) {
                    if (i == j) {
                        continue;
                    }
                    if (input[i][0] == input[j][0]) {
                        input[i][0] += ADDITIVE;
                        redo = true;
                    }
                    if (input[i][1] == input[j][1]) {
                        input[i][1] += ADDITIVE;
                        redo = true;
                    }
                }
            }
        } while (redo);
    }

    private double[][] getSortedPoints(int pointDIndex) {
        return new double[][]{
                input[0],
                input[((pointDIndex + 1) % 3) == 0 ? 3 : ((pointDIndex + 1) % 3)],
                input[((pointDIndex + 2) % 3) == 0 ? 3 : ((pointDIndex + 2) % 3)],
                input[pointDIndex]
        };
    }

    private Line[] getInitialLines(double[][] points) {
        // Create the initial lines, stored as
        // LineA (a-b), LineB (c-d), LineC (a-c) and LineD (b-d)
        return new Line[]{
                new Line(points[0], points[1]),
                new Line(points[2], points[3]),
                new Line(points[0], points[2]),
                new Line(points[1], points[3])
        };
    }

    private Line[] getRightAngledLines(Line[] initialLines) {

        // Get the angle of each line.
        double[] angles = getAngles(initialLines);
        double averageAngle = getAverageAngle(angles);

        return new Line[]{
                new Line(getSlope(averageAngle, 0), initialLines[0].getB()),
                new Line(getSlope(averageAngle, 0), initialLines[1].getB()),
                new Line(getSlope(averageAngle, -90), initialLines[2].getB()),
                new Line(getSlope(averageAngle, -90), initialLines[3].getB())
        };
    }

    private int getPointDIndex() {
        // The point of D is the only point that creates an intersection with lines A-D and B-C in de range [xA, xD].
        for (int i = 1; i < input.length; i++) {
            Line lineA = new Line(input[0], input[i]);
            Line lineB = new Line(input[(i + 1) % 3 == 0 ? 3 : (i + 1) % 3], input[(i + 2) % 3 == 0 ? 3 : (i + 2) % 3]);
            double[] intersect = Line.getIntersect(lineA, lineB);
            if (intersect != null && Math.abs(input[i][0] - intersect[0]) <= Math.abs(input[i][0] - input[0][0]) &&
                    Math.abs(input[i][1] - intersect[1]) <= Math.abs(input[i][1] - input[0][1])) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get the angles of the lines.
     * Add 90 degrees to the last 2 lines.
     * The range is 0 to 180 degrees.
     *
     * @param lines the 4 lines to get the angle of.
     * @return the angles, with range [0, 180)
     */
    private double[] getAngles(Line[] lines) {
        return new double[]{
                toDegreeAngle(lines[0].getA(), 0),
                toDegreeAngle(lines[1].getA(), 0),
                toDegreeAngle(lines[2].getA(), 90),
                toDegreeAngle(lines[3].getA(), 90)
        };
    }

    /**
     * Get the angle of the slope in degrees with range [0, 180)
     *
     * @param slope  the slope of a line
     * @param offset the offset in degrees to add
     * @return the angle in degrees with range [0, 180)
     */
    private double toDegreeAngle(double slope, double offset) {
        double degrees = Math.toDegrees(Math.atan(slope)) + offset;
        return (degrees + 180) % 180;
    }

    private double getAverageAngle(double[] angles) {
        // To take an average we need to make sure all angles are relatively near each other.
        // If an angle is at 1 degree and 179 degrees they are in reality close together but when taking an average
        // it'll make a complete mess.
        assert (angles.length == 4);
        Arrays.sort(angles);
        double min = angles[0];
        double max = angles[3];
        while (Math.abs(max - min) > 90) {
            angles[3] -= 180;
            Arrays.sort(angles);
            min = angles[0];
            max = angles[3];
        }
        return (Arrays.stream(angles).sum() / 4);
    }

    private double getSlope(double angle, double offset) {
        return Math.tan(Math.toRadians(angle + offset));
    }

    private void improveLinePlacement(Line[] lines, double[][] points) {
        boolean[] done = new boolean[4];
        Arrays.fill(done, true);
        boolean[] isDone = new boolean[]{false, false, false, false};

        int timeout = 100;

        // Get the points of each corner of the rectangle.
        while (!Arrays.equals(done, isDone) && timeout > 0) {

            // Line 1
            tryImprove(lines, points, isDone, 0, 2, 3, 0, 1);

            // Line 2
            tryImprove(lines, points, isDone, 1, 2, 3, 2, 3);

            // Line 3
            tryImprove(lines, points, isDone, 2, 0, 1, 0, 2);

            // Line 4
            tryImprove(lines, points, isDone, 3, 0, 1, 1, 3);
            timeout--;
        }
    }

    private double getSummedDistance(double[] cornerA, double[] pointA, double[] cornerB, double[] pointB) {
        return Point.distanceBetween(cornerA, pointA) + Point.distanceBetween(cornerB, pointB);
    }

    private void tryImprove(Line[] lines, double[][] points, boolean[] isDone,
                            int lineIndex, int lineIdxLeft, int lineIdxRight, int pointIdxLeft, int pointIdxRight) {
        double improve = 0;
        double tempImprove = ALPHA;
        // First try improving in the positive direction.
        while (canImprove(improve + tempImprove, points[pointIdxLeft], points[pointIdxRight], lines[lineIndex],
                lines[lineIdxLeft], lines[lineIdxRight])) {
            improve += tempImprove;
            tempImprove *= 2;
        }
        // Reset tempImprove
        tempImprove = ALPHA;
        // Then try improving in the negative direction, if not yet improved.
        while (canImprove(improve - tempImprove, points[pointIdxLeft], points[pointIdxRight], lines[lineIndex],
                lines[lineIdxLeft], lines[lineIdxRight])) {
            improve -= tempImprove;
            tempImprove *= 2;
        }
        // Update the B-value of the line with the improvement.
        // If there is no improvement set this line to done.
        if (improve == 0) {
            isDone[lineIndex] = true;
        } else {
            improve(improve, lines[lineIndex]);
        }
    }

    private boolean canImprove(double improvement, double[] pointA, double[] pointB, Line main, Line left, Line right) {
        double[] intersectA = Line.getIntersect(main, left);
        double[] intersectB = Line.getIntersect(main, right);

        // Offset the line and see if the distance between the intersections of the lines
        // and the initial points is better or worse than before.
        Line newMain = main.copyWithOffsetB(improvement);

        double[] newIntersectA = Line.getIntersect(newMain, left);
        double[] newIntersectB = Line.getIntersect(newMain, right);

        return getSummedDistance(intersectA, pointA, intersectB, pointB) > getSummedDistance(newIntersectA, pointA,
                newIntersectB, pointB);
    }

    private void improve(double improvement, Line main) {
        main.setB(main.getB() + improvement);
    }
}


