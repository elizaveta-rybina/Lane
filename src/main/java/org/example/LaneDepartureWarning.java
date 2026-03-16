package org.example;

/**
 * Система предупреждения о выезде с полосы движения (Lane Departure Warning - LDW).
 * 
 * Реализует алгоритм из статьи:
 * - Рассчитывает расстояние от колеса автомобиля до линии разметки
 * - Определяет, произошел ли выезд за пределы допустимого расстояния
 * - Подает сигнал тревоги при опасен отклонении
 */
public class LaneDepartureWarning {

    private final VehicleConfig vehicleConfig;
    private final double departureThreshold; // в метрах
    
    private boolean isLeftWarning = false;
    private boolean isRightWarning = false;
    private double leftDistance = Double.MAX_VALUE;
    private double rightDistance = Double.MAX_VALUE;

    /**
     * @param vehicleConfig конфигурация автомобиля (ширина полосы, ширина авто, и т.д.)
     * @param departureThreshold расстояние (в метрах) до линии, при превышении которого подается предупреждение
     */
    public LaneDepartureWarning(VehicleConfig vehicleConfig, double departureThreshold) {
        this.vehicleConfig = vehicleConfig;
        this.departureThreshold = departureThreshold;
    }

    /**
     * Обновляет состояние системы основываясь на текущем положении полос.
     * 
     * @param leftTopX X-координата верхней точки левой линии (в пикселях)
     * @param leftSlope наклон левой линии
     * @param rightTopX X-координата верхней точки правой линии (в пикселях)
     * @param rightSlope наклон правой линии
     * @param frameWidth ширина кадра (в пикселях)
     * @param frameHeight высота кадра (в пикселях)
     * @param pixelsPerMeter количество пикселей на один метр (для преобразования)
     */
    public void update(Double leftTopX, Double leftSlope,
                      Double rightTopX, Double rightSlope,
                      double frameWidth, double frameHeight,
                      double pixelsPerMeter) {
        
        isLeftWarning = false;
        isRightWarning = false;
        leftDistance = Double.MAX_VALUE;
        rightDistance = Double.MAX_VALUE;

        double centerX = frameWidth / 2.0;
        double vehicleBottomY = frameHeight; // предполагаем, что автомобиль внизу кадра
        double vehicleTopY = frameHeight * 0.85; // примерное расположение верхней части лобового стекла

        // Расчет расстояния для левой линии
        if (leftTopX != null && leftSlope != null && Math.abs(leftSlope) > 1e-6) {
            double leftLineXAtVehicleBottom = calculateLineXAtY(leftTopX, leftSlope, frameHeight * 0.65, vehicleBottomY);
            leftDistance = calculateDistanceToLine(centerX, leftLineXAtVehicleBottom, pixelsPerMeter);
            
            // Левое колесо находится на расстоянии vehicleHalfWidth влево от центра
            double leftWheelX = centerX - (vehicleConfig.getVehicleWidthMeters() / 2.0) * pixelsPerMeter;
            double leftWheelDistanceToLine = calculateDistanceToLine(leftWheelX, leftLineXAtVehicleBottom, pixelsPerMeter);
            
            if (leftWheelDistanceToLine < departureThreshold) {
                isLeftWarning = true;
                leftDistance = leftWheelDistanceToLine;
            }
        }

        // Расчет расстояния для правой линии
        if (rightTopX != null && rightSlope != null && Math.abs(rightSlope) > 1e-6) {
            double rightLineXAtVehicleBottom = calculateLineXAtY(rightTopX, rightSlope, frameHeight * 0.65, vehicleBottomY);
            rightDistance = calculateDistanceToLine(centerX, rightLineXAtVehicleBottom, pixelsPerMeter);
            
            // Правое колесо находится на расстоянии vehicleHalfWidth вправо от центра
            double rightWheelX = centerX + (vehicleConfig.getVehicleWidthMeters() / 2.0) * pixelsPerMeter;
            double rightWheelDistanceToLine = calculateDistanceToLine(rightWheelX, rightLineXAtVehicleBottom, pixelsPerMeter);
            
            if (rightWheelDistanceToLine < departureThreshold) {
                isRightWarning = true;
                rightDistance = rightWheelDistanceToLine;
            }
        }
    }

    /**
     * Рассчитывает X-координату на линии для заданного Y.
     * Используется уравнение линии: y = slope * x + b
     */
    private double calculateLineXAtY(double topX, double slope, double topY, double targetY) {
        double b = topY - slope * topX;
        return (targetY - b) / slope;
    }

    /**
     * Рассчитывает расстояние между точкой колеса и линией разметки (в метрах).
     * Формулы основаны на принципах компьютерного зрения и геометрии.
     */
    private double calculateDistanceToLine(double wheelX, double lineX, double pixelsPerMeter) {
        double pixelDistance = Math.abs(wheelX - lineX);
        return pixelDistance / pixelsPerMeter;
    }

    /**
     * @return true если левая сторона автомобиля опасно близко к левой разметке
     */
    public boolean isLeftWarning() {
        return isLeftWarning;
    }

    /**
     * @return true если правая сторона автомобиля опасно близко к правой разметке
     */
    public boolean isRightWarning() {
        return isRightWarning;
    }

    /**
     * @return true если произошел выезд с полосы с обеих сторон
     */
    public boolean isDeparture() {
        return isLeftWarning || isRightWarning;
    }

    /**
     * @return расстояние (в метрах) от левого колеса до левой разметки
     */
    public double getLeftDistance() {
        return leftDistance;
    }

    /**
     * @return расстояние (в метрах) от правого колеса до правой разметки
     */
    public double getRightDistance() {
        return rightDistance;
    }

    /**
     * @return минимальное расстояние от колеса до ближайшей линии разметки
     */
    public double getMinDistance() {
        return Math.min(leftDistance, rightDistance);
    }

    /**
     * @return установленный порог предупреждения (в метрах)
     */
    public double getDepartureThreshold() {
        return departureThreshold;
    }
}
