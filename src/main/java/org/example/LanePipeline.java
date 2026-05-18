package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;
import java.util.List;

public class LanePipeline {
    
    private final LaneSmoother smoother;
    private final LaneDepartureWarning ldw;
    private double pixelsPerMeter = 40.0; 

    private Mat M = null;
    private Mat invM = null;

    // Храним текущие значения калибровки
    private double currentTopY = 0.65;
    private double currentTopWidth = 0.10;
    private double currentBottomWidth = 0.80;

    public LanePipeline(double smoothingAlpha, VehicleConfig vehicleConfig, double departureThreshold) {
        this.smoother = new LaneSmoother(smoothingAlpha);
        this.ldw = new LaneDepartureWarning(vehicleConfig, departureThreshold);
    }

    // Метод обновления параметров с ползунков
    public void updateCalibration(double topY, double topWidth, double bottomWidth) {
        // Если значения изменились, обнуляем матрицы для их пересчета
        if (this.currentTopY != topY || this.currentTopWidth != topWidth || this.currentBottomWidth != bottomWidth) {
            this.currentTopY = topY;
            this.currentTopWidth = topWidth;
            this.currentBottomWidth = bottomWidth;
            
            if (M != null) { M.release(); M = null; }
            if (invM != null) { invM.release(); invM = null; }
        }
    }

    public LaneEstimate process(Mat frame) {
        double width = frame.width();
        double height = frame.height();

        if (M == null || invM == null) {
            M = LaneUtils.getPerspectiveTransformMatrix(width, height, currentTopY, currentTopWidth, currentBottomWidth, false);
            invM = LaneUtils.getPerspectiveTransformMatrix(width, height, currentTopY, currentTopWidth, currentBottomWidth, true);
        }

        Mat binaryMask = LaneUtils.advancedThresholding(frame);
        
        Mat warped = new Mat();
        Imgproc.warpPerspective(binaryMask, warped, M, new Size(width, height), Imgproc.INTER_LINEAR);

        int nwindows = 9;
        int margin = 100;
        int minpix = 50;
        int windowHeight = (int) (height / nwindows);
        
        Mat bottomHalf = warped.submat((int) (height / 2), (int) height, 0, (int) width);
        Mat hist = new Mat();
        Core.reduce(bottomHalf, hist, 0, Core.REDUCE_SUM, CvType.CV_32S);
        
        Point minMaxLeft = Core.minMaxLoc(hist.colRange(0, (int) (width / 2))).maxLoc;
        Point minMaxRight = Core.minMaxLoc(hist.colRange((int) (width / 2), (int) width)).maxLoc;
        
        int leftBase = (int) minMaxLeft.x;
        int rightBase = (int) minMaxRight.x + (int) (width / 2);
        
        List<Point> leftPixels = new ArrayList<>();
        List<Point> rightPixels = new ArrayList<>();

        Mat nonZero = new Mat();
        Core.findNonZero(warped, nonZero);
        
        int totalPoints = (int) nonZero.total();
        int[] pointsData = new int[totalPoints * 2]; 
        if (totalPoints > 0) {
            nonZero.get(0, 0, pointsData);
        }
        
        int leftCurrentX = leftBase;
        int rightCurrentX = rightBase;
        
        for (int w = 0; w < nwindows; w++) {
            int winYLow = (int) (height - (w + 1) * windowHeight);
            int winYHigh = (int) (height - w * windowHeight);
            
            int winXLeftLow = leftCurrentX - margin;
            int winXLeftHigh = leftCurrentX + margin;
            int winXRightLow = rightCurrentX - margin;
            int winXRightHigh = rightCurrentX + margin;
            
            List<Point> goodLeft = new ArrayList<>();
            List<Point> goodRight = new ArrayList<>();
            
            for (int i = 0; i < totalPoints; i++) {
                int px = pointsData[i * 2];
                int py = pointsData[i * 2 + 1];
                
                if (py >= winYLow && py < winYHigh) {
                    if (px >= winXLeftLow && px < winXLeftHigh) goodLeft.add(new Point(px, py));
                    if (px >= winXRightLow && px < winXRightHigh) goodRight.add(new Point(px, py));
                }
            }
            
            leftPixels.addAll(goodLeft);
            rightPixels.addAll(goodRight);
            
            if (goodLeft.size() > minpix) leftCurrentX = (int) getMeanX(goodLeft);
            if (goodRight.size() > minpix) rightCurrentX = (int) getMeanX(goodRight);
        }

        double[] leftPoly = polyFit(leftPixels, 2);
        double[] rightPoly = polyFit(rightPixels, 2);
        
        binaryMask.release(); warped.release(); bottomHalf.release(); hist.release(); nonZero.release();
        
        smoother.updateLeft(leftPoly);
        smoother.updateRight(rightPoly);

        LaneEstimate estimate = new LaneEstimate(
            smoother.getSmoothLeftPoly(),
            smoother.getSmoothRightPoly(),
            invM, width, height
        );

        ldw.update(estimate.leftPoly(), estimate.rightPoly(), width, height, pixelsPerMeter);
        return estimate;
    }
    
    private double getMeanX(List<Point> pts) {
        double sum = 0;
        for (Point p : pts) sum += p.x;
        return sum / pts.size();
    }
    
    private double[] polyFit(List<Point> points, int degree) {
        if (points.size() <= degree) return null;
        
        Mat A = new Mat(points.size(), degree + 1, CvType.CV_64F);
        Mat B = new Mat(points.size(), 1, CvType.CV_64F);
        
        for (int i = 0; i < points.size(); i++) {
            double y = points.get(i).y;
            double x = points.get(i).x;
            B.put(i, 0, x);
            for (int j = 0; j <= degree; j++) {
                A.put(i, j, Math.pow(y, degree - j));
            }
        }
        
        Mat result = new Mat();
        Core.solve(A, B, result, Core.DECOMP_SVD);
        
        double[] coeffs = new double[degree + 1];
        for (int i = 0; i <= degree; i++) coeffs[i] = result.get(i, 0)[0];
        
        A.release(); B.release(); result.release();
        return coeffs;
    }

    public boolean isDeparture() { return ldw.isDeparture(); }
    public double getMinDistance() { return ldw.getMinDistance(); }
    public LaneDepartureWarning getLDW() { return ldw; }
    
    public void release() {
        if (M != null) M.release();
        if (invM != null) invM.release();
    }
}