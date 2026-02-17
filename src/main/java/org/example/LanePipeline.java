package org.example;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Пайплайн обработки кадра: фильтрация, детекция линий и обновление сглаженного состояния.
 */
public class LanePipeline {

    private final Mat gray = new Mat();
    private final Mat edges = new Mat();
    private final Mat lines = new Mat();
    private final Mat maskedEdges = new Mat();

    private final LaneSmoother smoother;

    public LanePipeline(double smoothingAlpha) {
        this.smoother = new LaneSmoother(smoothingAlpha);
    }

    public LaneEstimate process(Mat frame) {
        double width = frame.width();
        double height = frame.height();
        double centerX = width / 2.0;

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

        return new LaneEstimate(
                smoother.getSmoothLeftTopX(),
                smoother.getSmoothLeftSlope(),
                smoother.getSmoothRightTopX(),
                smoother.getSmoothRightSlope(),
                width,
                height
        );
    }

    public void release() {
        gray.release();
        edges.release();
        lines.release();
        maskedEdges.release();
    }
}
