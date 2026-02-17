package org.example;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилиты для операций с ROI и вспомогательной обработки кадров.
 */
public class LaneUtils {

    /**
     * Применяет трапецеидальную маску, покрывающую нижние 40% кадра.
     *
     * @param source      исходная матрица (обычно карта границ)
     * @param destination выходная матрица после применения маски
     */
    public static void maskBottom40Percent(Mat source, Mat destination) {
        Mat mask = Mat.zeros(source.size(), source.type());

        int w = source.width();
        int h = source.height();

        int topY = (int) (h * 0.60);

        Point p1 = new Point(0, h);
        Point p2 = new Point(w, h);
        Point p3 = new Point(w * 0.90, topY);
        Point p4 = new Point(w * 0.10, topY);

        MatOfPoint polygon = new MatOfPoint(p1, p2, p3, p4);
        List<MatOfPoint> list = new ArrayList<>();
        list.add(polygon);

        Imgproc.fillPoly(mask, list, new Scalar(255));

        Core.bitwise_and(source, mask, destination);

        mask.release();
        polygon.release();
    }
}