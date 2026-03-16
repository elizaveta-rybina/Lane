package org.example;

/**
 * Конфигурация параметров автомобиля для расчета расстояния до разметки.
 * 
 * Хранит физические параметры автомобиля, необходимые для точного расчета
 * расстояния от колес до линий разметки.
 */
public class VehicleConfig {

    // Типичные параметры среднего автомобиля (в метрах)
    private final double vehicleWidthMeters;        // Ширина автомобиля (примерно 1.8-2.0 м)
    private final double wheelbaseMeters;           // Расстояние между осями (примерно 2.5-3.0 м)
    private final double vehicleHeightMeters;       // Высота автомобиля (примерно 1.5 м)
    private final double cameraHeightMeters;        // Высота камеры над дорогой (примерно 1.2 м)
    private final double laneWidthMeters;           // Ширина дорожной полосы (примерно 3.7-3.75 м по стандартам)

    /**
     * Конструктор с параметрами по умолчанию для среднего автомобиля.
     */
    public VehicleConfig() {
        this(1.9, 2.7, 1.55, 1.2, 3.7);
    }

    /**
     * Конструктор с пользовательскими параметрами.
     *
     * @param vehicleWidthMeters ширина автомобиля (м)
     * @param wheelbaseMeters расстояние между осями (м)
     * @param vehicleHeightMeters высота автомобиля (м)
     * @param cameraHeightMeters высота установки камеры над дорогой (м)
     * @param laneWidthMeters ширина дорожной полосы (м)
     */
    public VehicleConfig(double vehicleWidthMeters, double wheelbaseMeters,
                        double vehicleHeightMeters, double cameraHeightMeters,
                        double laneWidthMeters) {
        this.vehicleWidthMeters = vehicleWidthMeters;
        this.wheelbaseMeters = wheelbaseMeters;
        this.vehicleHeightMeters = vehicleHeightMeters;
        this.cameraHeightMeters = cameraHeightMeters;
        this.laneWidthMeters = laneWidthMeters;
    }

    /**
     * Ширина автомобиля в метрах.
     */
    public double getVehicleWidthMeters() {
        return vehicleWidthMeters;
    }

    /**
     * Расстояние между осями (wheelbase) в метрах.
     */
    public double getWheelbaseMeters() {
        return wheelbaseMeters;
    }

    /**
     * Высота автомобиля в метрах.
     */
    public double getVehicleHeightMeters() {
        return vehicleHeightMeters;
    }

    /**
     * Высота установки камеры над дорогой в метрах.
     * Влияет на точность расчетов при преобразовании пиксели -> метры.
     */
    public double getCameraHeightMeters() {
        return cameraHeightMeters;
    }

    /**
     * Ширина дорожной полосы в метрах.
     */
    public double getLaneWidthMeters() {
        return laneWidthMeters;
    }

    /**
     * Валидирует параметры конфигурации.
     * @return true если все параметры в допустимых диапазонах
     */
    public boolean isValid() {
        return vehicleWidthMeters > 0 && vehicleWidthMeters < 3.0 &&
               wheelbaseMeters > 0 && wheelbaseMeters < 5.0 &&
               vehicleHeightMeters > 0 && vehicleHeightMeters < 3.0 &&
               cameraHeightMeters > 0 && cameraHeightMeters < 2.0 &&
               laneWidthMeters > 0 && laneWidthMeters < 6.0;
    }

    @Override
    public String toString() {
        return String.format(
                "VehicleConfig{width=%.2fm, wheelbase=%.2fm, height=%.2fm, cameraHeight=%.2fm, laneWidth=%.2fm}",
                vehicleWidthMeters, wheelbaseMeters, vehicleHeightMeters, cameraHeightMeters, laneWidthMeters
        );
    }
}
