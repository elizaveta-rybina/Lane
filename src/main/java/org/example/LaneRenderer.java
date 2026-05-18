package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Отвечает за отрисовку полиномиальных кривых полос движения и информации о LDW.
 */
public class LaneRenderer {

    public void render(Mat frame, LaneEstimate estimate) {
        render(frame, estimate, null);
    }

    public void render(Mat frame, LaneEstimate estimate, LaneDepartureWarning ldw) {
        if (estimate == null || estimate.invM() == null) return;

        double[] leftPoly = estimate.leftPoly();
        double[] rightPoly = estimate.rightPoly();
        double width = estimate.frameWidth();
        double height = estimate.frameHeight();

        Mat overlay = frame.clone();
        int step = 20; // Шаг по Y

        List<Point> leftPoints = new ArrayList<>();
        List<Point> rightPoints = new ArrayList<>();

        if (leftPoly != null) {
            for (double y = 0; y <= height; y += step) {
                double x = evaluatePoly(leftPoly, y);
                leftPoints.add(new Point(x, y));
            }
            leftPoints.add(new Point(evaluatePoly(leftPoly, height), height));
        }

        if (rightPoly != null) {
            for (double y = 0; y <= height; y += step) {
                double x = evaluatePoly(rightPoly, y);
                rightPoints.add(new Point(x, y));
            }
            rightPoints.add(new Point(evaluatePoly(rightPoly, height), height));
        }

        // Если обе линии найдены, рисуем зелёный полигон между ними
        if (leftPoly != null && rightPoly != null) {
            List<Point> polygonPoints = new ArrayList<>(leftPoints);
            
            // Добавляем правые точки в обратном порядке от низа к верху
            List<Point> reversedRight = new ArrayList<>(rightPoints);
            Collections.reverse(reversedRight);
            polygonPoints.addAll(reversedRight);

            MatOfPoint2f warpedPoly = new MatOfPoint2f();
            warpedPoly.fromList(polygonPoints);

            MatOfPoint2f unwarpedPoly = new MatOfPoint2f();
            Core.perspectiveTransform(warpedPoly, unwarpedPoly, estimate.invM());

            MatOfPoint unwarpedPolyInt = new MatOfPoint();
            unwarpedPoly.convertTo(unwarpedPolyInt, CvType.CV_32S);

            Imgproc.fillPoly(overlay, Collections.singletonList(unwarpedPolyInt), new Scalar(0, 255, 0));

            warpedPoly.release();
            unwarpedPoly.release();
            unwarpedPolyInt.release();
        }

        // Отрисовка самих линий
        if (leftPoly != null) {
            drawCurve(overlay, leftPoints, estimate.invM(), new Scalar(255, 100, 0));
        }
        if (rightPoly != null) {
            drawCurve(overlay, rightPoints, estimate.invM(), new Scalar(0, 100, 255));
        }

        Core.addWeighted(frame, 1.0, overlay, 0.3, 0, frame);
        overlay.release();

        // Отрисовываем визуализацию Lane Departure Warning
        if (ldw != null && ldw.isDeparture()) {
            renderLDWWarning(frame, ldw, width, height);
        }
    }

    private void drawCurve(Mat img, List<Point> pts, Mat invM, Scalar color) {
        if (pts.isEmpty()) return;
        MatOfPoint2f warpedLine = new MatOfPoint2f();
        warpedLine.fromList(pts);

        MatOfPoint2f unwarpedLine = new MatOfPoint2f();
        Core.perspectiveTransform(warpedLine, unwarpedLine, invM);

        MatOfPoint unwarpedLineInt = new MatOfPoint();
        unwarpedLine.convertTo(unwarpedLineInt, CvType.CV_32S);

        List<MatOfPoint> polylines = new ArrayList<>();
        polylines.add(unwarpedLineInt);
        
        Imgproc.polylines(img, polylines, false, color, 8);

        warpedLine.release();
        unwarpedLine.release();
        unwarpedLineInt.release();
    }

    private double evaluatePoly(double[] poly, double y) {
        return poly[0] * Math.pow(y, 2) + poly[1] * y + poly[2];
    }

    private void renderLDWWarning(Mat frame, LaneDepartureWarning ldw, double width, double height) {
        Scalar warningColor = new Scalar(0, 0, 255);
        Imgproc.rectangle(frame, new Point(10, 10), new Point(width - 10, height - 10), warningColor, 6);
        Imgproc.putText(frame, "LANE DEPARTURE WARNING", new Point(width / 2 - 200, 50), Imgproc.FONT_HERSHEY_SIMPLEX, 1.2, warningColor, 3);

        if (ldw.isLeftWarning()) Imgproc.putText(frame, "LEFT LANE DEPARTURE", new Point(30, 100), Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, warningColor, 2);
        if (ldw.isRightWarning()) Imgproc.putText(frame, "RIGHT LANE DEPARTURE", new Point(width - 350, 100), Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, warningColor, 2);

        double minDistance = ldw.getMinDistance();
        if (minDistance < Double.MAX_VALUE) {
            String distanceText = String.format("MIN DISTANCE: %.2f m", minDistance);
            Imgproc.putText(frame, distanceText, new Point(30, (int)height - 30), Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, warningColor, 2);
        }
    }
}