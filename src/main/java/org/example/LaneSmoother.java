package org.example;

/**
 * Хранит и сглаживает коэффициенты полиномов полос между кадрами (X = A*Y^2 + B*Y + C).
 */
public class LaneSmoother {

    private final double alpha;

    private double[] smoothLeftPoly;
    private double[] smoothRightPoly;

    public LaneSmoother(double alpha) {
        this.alpha = alpha;
    }

    public void updateLeft(double[] currentPoly) {
        if (currentPoly == null) return;
        if (smoothLeftPoly == null) {
            smoothLeftPoly = currentPoly.clone();
            return;
        }
        for (int i = 0; i < 3; i++) {
            smoothLeftPoly[i] = blend(smoothLeftPoly[i], currentPoly[i]);
        }
    }

    public void updateRight(double[] currentPoly) {
        if (currentPoly == null) return;
        if (smoothRightPoly == null) {
            smoothRightPoly = currentPoly.clone();
            return;
        }
        for (int i = 0; i < 3; i++) {
            smoothRightPoly[i] = blend(smoothRightPoly[i], currentPoly[i]);
        }
    }

    public double[] getSmoothLeftPoly() {
        return smoothLeftPoly;
    }

    public double[] getSmoothRightPoly() {
        return smoothRightPoly;
    }

    private double blend(double previous, double current) {
        return previous * (1 - alpha) + current * alpha;
    }
}