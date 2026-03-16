package org.example;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Пайплайн обработки кадра: фильтрация, детекция линий и обновление сглаженного состояния.
 * Включает систему предупреждения о выезде с полосы (Lane Departure Warning).
 */
public class LanePipeline {

    private final Mat gray = new Mat();
    private final Mat edges = new Mat();
    private final Mat lines = new Mat();
    private final Mat maskedEdges = new Mat();

    private final LaneSmoother smoother;
    private final LaneDepartureWarning ldw;
    
    // Параметры для преобразования пиксели -> метры
    // Рассчитываются на основе размеров видео и конфигурации камеры
    private double pixelsPerMeter = 40.0; // значение по умолчанию

    /**
     * @param smoothingAlpha коэффициент сглаживания (0.0 - 1.0)
     * @param vehicleConfig конфигурация параметров автомобиля
     * @param departureThreshold порог расстояния для срабатывания предупреждения (в метрах)
     */
    public LanePipeline(double smoothingAlpha, VehicleConfig vehicleConfig, double departureThreshold) {
        this.smoother = new LaneSmoother(smoothingAlpha);
        this.ldw = new LaneDepartureWarning(vehicleConfig, departureThreshold);
    }

    /**
     * Конструктор с параметрами по умолчанию.
     * @param smoothingAlpha коэффициент сглаживания
     */
    public LanePipeline(double smoothingAlpha) {
        this(smoothingAlpha, new VehicleConfig(), 0.3); // порог по умолчанию 0.3 метра
    }

    public LaneEstimate process(Mat frame) {
        double width = frame.width();
        double height = frame.height();
        double centerX = width / 2.0;

        // Рассчитываем pixelsPerMeter один раз (примерно)
        // Базируется на предположении, что видео снято с высоты ~1.2 м под углом ~45 градусов
        calculatePixelsPerMeter(width, height);

        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
        Imgproc.Canny(gray, edges, 50, 150);
        LaneUtils.maskBottom40Percent(edges, maskedEdges);
        Imgproc.HoughLinesP(maskedEdges, lines, 1, Math.PI / 180, 50, 50, 10);

        double leftXSum = 0;
        double leftYSum = 0;
        double leftSlopeSum = 0;
        int leftCount = 0;

        double rightXSum = 0;
        double rightYSum = 0;
        double rightSlopeSum = 0;
        int rightCount = 0;

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            double x1 = line[0];
            double y1 = line[1];
            double x2 = line[2];
            double y2 = line[3];

            if (x2 == x1) {
                continue;
            }

            double slope = (y2 - y1) / (x2 - x1);
            if (Math.abs(slope) < 0.4) {
                continue;
            }

            if (slope < 0 && x1 < centerX && x2 < centerX) {
                leftXSum += (x1 + x2);
                leftYSum += (y1 + y2);
                leftSlopeSum += slope;
                leftCount += 2;
            } else if (slope > 0 && x1 > centerX && x2 > centerX) {
                rightXSum += (x1 + x2);
                rightYSum += (y1 + y2);
                rightSlopeSum += slope;
                rightCount += 2;
            }
        }

        if (leftCount > 0) {
            double avgX = leftXSum / leftCount;
            double avgY = leftYSum / leftCount;
            double currentSlope = leftSlopeSum / (leftCount / 2.0);

            double b = avgY - currentSlope * avgX;
            double currentTopX = (height * 0.65 - b) / currentSlope;
            smoother.updateLeft(currentTopX, currentSlope);
        }

        if (rightCount > 0) {
            double avgX = rightXSum / rightCount;
            double avgY = rightYSum / rightCount;
            double currentSlope = rightSlopeSum / (rightCount / 2.0);

            double b = avgY - currentSlope * avgX;
            double currentTopX = (height * 0.65 - b) / currentSlope;
            smoother.updateRight(currentTopX, currentSlope);
        }

        LaneEstimate estimate = new LaneEstimate(
                smoother.getSmoothLeftTopX(),
                smoother.getSmoothLeftSlope(),
                smoother.getSmoothRightTopX(),
                smoother.getSmoothRightSlope(),
                width,
                height
        );

        // Обновляем систему LDW
        ldw.update(
                estimate.leftTopX(), estimate.leftSlope(),
                estimate.rightTopX(), estimate.rightSlope(),
                width, height, pixelsPerMeter
        );

        return estimate;
    }

    /**
     * Рассчитывает примерное количество пикселей на метр на основе размеров кадра.
     * Это приблизительный расчет, основанный на типовых параметрах видеокамеры.
     */
    private void calculatePixelsPerMeter(double frameWidth, double frameHeight) {
        // Типовое соотношение: для HD видео (640x480 или 1280x720) примерно 30-50 пикселей на метр
        // Корректируем на основе высоты кадра
        double basePPM = 40.0;
        pixelsPerMeter = basePPM * (frameHeight / 480.0); // нормализуем по HD высоте
    }

    /**
     * @return объект системы Lane Departure Warning
     */
    public LaneDepartureWarning getLDW() {
        return ldw;
    }

    /**
     * @return true если произошел выезд с полосы
     */
    public boolean isDeparture() {
        return ldw.isDeparture();
    }

    /**
     * @return true если левая сторона в опасности
     */
    public boolean isLeftWarning() {
        return ldw.isLeftWarning();
    }

    /**
     * @return true если правая сторона в опасности
     */
    public boolean isRightWarning() {
        return ldw.isRightWarning();
    }

    /**
     * @return минимальное расстояние до линии разметки (в метрах)
     */
    public double getMinDistance() {
        return ldw.getMinDistance();
    }

    public void release() {
        gray.release();
        edges.release();
        lines.release();
        maskedEdges.release();
    }
}
