package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;

/**
 * Отвечает за отрисовку полос движения, направляющей стрелки и информации о LDW.
 */
public class LaneRenderer {

    public void render(Mat frame, LaneEstimate estimate) {
        render(frame, estimate, null);
    }

    /**
     * Отрисовывает полосы, стрелку и визуализирует предупреждение о выезде с полосы.
     *
     * @param frame изображение для отрисовки
     * @param estimate оценка положения полос
     * @param ldw система Lane Departure Warning (может быть null)
     */
    public void render(Mat frame, LaneEstimate estimate, LaneDepartureWarning ldw) {
        Mat overlay = frame.clone();

        Double leftTopX = estimate.leftTopX();
        Double leftSlope = estimate.leftSlope();
        Double rightTopX = estimate.rightTopX();
        Double rightSlope = estimate.rightSlope();

        double width = estimate.frameWidth();
        double height = estimate.frameHeight();
        double centerX = width / 2.0;

        if (leftTopX != null && leftSlope != null && Math.abs(leftSlope) > 1e-6) {
            double y1 = height;
            double y2 = height * 0.65;
            double x2 = leftTopX;
            double x1 = x2 - (y2 - y1) / leftSlope;

            Imgproc.line(overlay, new Point(x1, y1), new Point(x2, y2), new Scalar(255, 100, 0), 8);
        }

        if (rightTopX != null && rightSlope != null && Math.abs(rightSlope) > 1e-6) {
            double y1 = height;
            double y2 = height * 0.65;
            double x2 = rightTopX;
            double x1 = x2 - (y2 - y1) / rightSlope;

            Imgproc.line(overlay, new Point(x1, y1), new Point(x2, y2), new Scalar(0, 100, 255), 8);
        }

        if (leftTopX != null && rightTopX != null
                && leftSlope != null && rightSlope != null
                && Math.abs(leftSlope) > 1e-6 && Math.abs(rightSlope) > 1e-6) {

            double yTop = height * 0.65;
            double yBot = height;

            Point p1 = new Point(leftTopX - (yTop - yBot) / leftSlope, yBot);
            Point p2 = new Point(leftTopX, yTop);
            Point p3 = new Point(rightTopX, yTop);
            Point p4 = new Point(rightTopX - (yTop - yBot) / rightSlope, yBot);

            MatOfPoint poly = new MatOfPoint(p1, p2, p3, p4);
            Imgproc.fillPoly(overlay, Collections.singletonList(poly), new Scalar(0, 255, 0));
            poly.release();
        }

        Core.addWeighted(frame, 1.0, overlay, 0.3, 0, frame);
        overlay.release();

        double targetX = centerX;
        if (leftTopX != null && rightTopX != null) {
            targetX = (leftTopX + rightTopX) / 2.0;
        } else if (leftTopX != null) {
            targetX = leftTopX + (width * 0.15);
        } else if (rightTopX != null) {
            targetX = rightTopX - (width * 0.15);
        }

        Imgproc.arrowedLine(frame,
                new Point(centerX, height * 0.9),
                new Point(targetX, height * 0.7),
                new Scalar(0, 255, 100),
                3,
                8,
                0,
                0.2);

        // Отрисовываем визуализацию Lane Departure Warning если она активна
        if (ldw != null && ldw.isDeparture()) {
            renderLDWWarning(frame, ldw, width, height);
        }
    }

    /**
     * Отрисовывает визуальное предупреждение о выезде с полосы.
     */
    private void renderLDWWarning(Mat frame, LaneDepartureWarning ldw, double width, double height) {
        // Красный прямоугольник с мигающим эффектом
        Scalar warningColor = new Scalar(0, 0, 255); // красный цвет (BGR)

        // Рисуем толстую красную границу
        Imgproc.rectangle(frame,
                new Point(10, 10),
                new Point(width - 10, height - 10),
                warningColor,
                6);

        // Текстовое предупреждение
        String warningText = "⚠ LANE DEPARTURE WARNING ⚠";
        Imgproc.putText(frame, warningText,
                new Point(width / 2 - 200, 50),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                1.2,
                warningColor,
                3);

        // Информация о стороне опасности
        if (ldw.isLeftWarning()) {
            Imgproc.putText(frame, "LEFT LANE DEPARTURE",
                    new Point(30, 100),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    warningColor,
                    2);
        }

        if (ldw.isRightWarning()) {
            Imgproc.putText(frame, "RIGHT LANE DEPARTURE",
                    new Point(width - 350, 100),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    warningColor,
                    2);
        }

        // Информация о расстоянии
        double minDistance = ldw.getMinDistance();
        if (minDistance < Double.MAX_VALUE) {
            String distanceText = String.format("MIN DISTANCE: %.2f m", minDistance);
            Imgproc.putText(frame, distanceText,
                    new Point(30, (int)height - 30),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.8,
                    warningColor,
                    2);
        }
    }
}

