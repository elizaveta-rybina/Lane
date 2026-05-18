package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class LaneUtils {

    public static Mat advancedThresholding(Mat frame) {
        Mat hls = new Mat();
        Imgproc.cvtColor(frame, hls, Imgproc.COLOR_BGR2HLS);

        Mat lChannel = new Mat();
        Core.extractChannel(hls, lChannel, 1);
        Mat whiteMask = new Mat();
        Imgproc.threshold(lChannel, whiteMask, 200, 255, Imgproc.THRESH_BINARY);

        Mat yellowMask = new Mat();
        Core.inRange(hls, new Scalar(15, 30, 115), new Scalar(35, 204, 255), yellowMask);

        Mat sobelX = new Mat();
        Imgproc.Sobel(lChannel, sobelX, CvType.CV_64F, 1, 0, 3, 1, 0, Core.BORDER_DEFAULT);
        Core.convertScaleAbs(sobelX, sobelX);
        Mat sobelMask = new Mat();
        Imgproc.threshold(sobelX, sobelMask, 30, 255, Imgproc.THRESH_BINARY);

        Mat combined = new Mat();
        Core.bitwise_or(whiteMask, yellowMask, combined);
        Core.bitwise_or(combined, sobelMask, combined);

        hls.release(); lChannel.release(); whiteMask.release(); 
        yellowMask.release(); sobelX.release(); sobelMask.release();

        return combined;
    }

    /**
     * Динамический расчет матриц перспективы
     */
    public static Mat getPerspectiveTransformMatrix(double w, double h, double topY, double topWidth, double bottomWidth, boolean inverse) {
        
        double topLeftX = 0.5 - (topWidth / 2.0);
        double topRightX = 0.5 + (topWidth / 2.0);
        double bottomLeftX = 0.5 - (bottomWidth / 2.0);
        double bottomRightX = 0.5 + (bottomWidth / 2.0);

        Point[] src = new Point[]{
            new Point(w * topLeftX, h * topY),     
            new Point(w * topRightX, h * topY),    
            new Point(w * bottomLeftX, h),         
            new Point(w * bottomRightX, h)         
        };
        
        // В проекции сверху линии делаем идеально параллельными (отступаем 20% от краев)
        Point[] dst = new Point[]{
            new Point(w * 0.2, 0),         
            new Point(w * 0.8, 0),         
            new Point(w * 0.2, h),         
            new Point(w * 0.8, h)          
        };
        
        MatOfPoint2f srcMat = new MatOfPoint2f(src);
        MatOfPoint2f dstMat = new MatOfPoint2f(dst);

        if (inverse) {
             return Imgproc.getPerspectiveTransform(dstMat, srcMat);
        }
        return Imgproc.getPerspectiveTransform(srcMat, dstMat);
    }
}