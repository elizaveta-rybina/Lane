package org.example;

/**
 * Система предупреждения о выезде с полосы движения (Lane Departure Warning - LDW).
 * Использует полиномы для расчёта расстояния в перспективе Bird's-Eye View.
 */
public class LaneDepartureWarning {

    private final VehicleConfig vehicleConfig;
    private final double departureThreshold; // в метрах
    
    private boolean isLeftWarning = false;
    private boolean isRightWarning = false;
    private double leftDistance = Double.MAX_VALUE;
    private double rightDistance = Double.MAX_VALUE;

    public LaneDepartureWarning(VehicleConfig vehicleConfig, double departureThreshold) {
        this.vehicleConfig = vehicleConfig;
        this.departureThreshold = departureThreshold;
    }

    public void update(double[] leftPoly, double[] rightPoly,
                      double frameWidth, double frameHeight,
                      double pixelsPerMeter) {
        
        isLeftWarning = false;
        isRightWarning = false;
        leftDistance = Double.MAX_VALUE;
        rightDistance = Double.MAX_VALUE;

        double centerX = frameWidth / 2.0;
        double vehicleBottomY = frameHeight; // Автомобиль находится в самом низу кадра Bird's-Eye View

        // Половина ширины автомобиля в пикселях
        double vehicleHalfWidthPx = (vehicleConfig.getVehicleWidthMeters() / 2.0) * pixelsPerMeter;

        if (leftPoly != null && leftPoly.length >= 3) {
            double leftLineX = evaluatePoly(leftPoly, vehicleBottomY);
            
            // Левое колесо относительно центра
            double leftWheelX = centerX - vehicleHalfWidthPx;
            
            double pixelDistance = Math.abs(leftWheelX - leftLineX);
            double lineMeters = pixelDistance / pixelsPerMeter;
            leftDistance = lineMeters;
            
            if (lineMeters < departureThreshold) {
                isLeftWarning = true;
            }
        }

        if (rightPoly != null && rightPoly.length >= 3) {
            double rightLineX = evaluatePoly(rightPoly, vehicleBottomY);
            
            // Правое колесо относительно центра
            double rightWheelX = centerX + vehicleHalfWidthPx;
            
            double pixelDistance = Math.abs(rightWheelX - rightLineX);
            double lineMeters = pixelDistance / pixelsPerMeter;
            rightDistance = lineMeters;
            
            if (lineMeters < departureThreshold) {
                isRightWarning = true;
            }
        }
    }

    private double evaluatePoly(double[] poly, double y) {
        return poly[0] * Math.pow(y, 2) + poly[1] * y + poly[2];
    }

    public boolean isLeftWarning() { return isLeftWarning; }
    public boolean isRightWarning() { return isRightWarning; }
    public boolean isDeparture() { return isLeftWarning || isRightWarning; }
    public double getLeftDistance() { return leftDistance; }
    public double getRightDistance() { return rightDistance; }
    public double getMinDistance() { return Math.min(leftDistance, rightDistance); }
    public double getDepartureThreshold() { return departureThreshold; }
}