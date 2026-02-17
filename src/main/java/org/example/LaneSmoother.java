package org.example;

/**
 * Хранит и сглаживает параметры полос между кадрами.
 */
public class LaneSmoother {

    private final double alpha;

    private Double smoothLeftTopX;
    private Double smoothRightTopX;
    private Double smoothLeftSlope;
    private Double smoothRightSlope;

    public LaneSmoother(double alpha) {
        this.alpha = alpha;
    }

    public void updateLeft(double currentTopX, double currentSlope) {
        if (smoothLeftTopX == null) {
            smoothLeftTopX = currentTopX;
            smoothLeftSlope = currentSlope;
            return;
        }

        smoothLeftTopX = blend(smoothLeftTopX, currentTopX);
        smoothLeftSlope = blend(smoothLeftSlope, currentSlope);
    }

    public void updateRight(double currentTopX, double currentSlope) {
        if (smoothRightTopX == null) {
            smoothRightTopX = currentTopX;
            smoothRightSlope = currentSlope;
            return;
        }

        smoothRightTopX = blend(smoothRightTopX, currentTopX);
        smoothRightSlope = blend(smoothRightSlope, currentSlope);
    }

    public Double getSmoothLeftTopX() {
        return smoothLeftTopX;
    }

    public Double getSmoothRightTopX() {
        return smoothRightTopX;
    }

    public Double getSmoothLeftSlope() {
        return smoothLeftSlope;
    }

    public Double getSmoothRightSlope() {
        return smoothRightSlope;
    }

    private double blend(double previous, double current) {
        return previous * (1 - alpha) + current * alpha;
    }
}
