package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;

/**
 * Отвечает за отрисовку полос движения и направляющей стрелки.
 */
public class LaneRenderer {

    public void render(Mat frame, LaneEstimate estimate) {
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
    }
}
