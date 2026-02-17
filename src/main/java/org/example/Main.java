package org.example;

import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        OpenCV.loadLocally();

        // Укажи путь к видео или 0 для веб-камеры
        VideoCapture capture = new VideoCapture("road.mp4");

        if (!capture.isOpened()) {
            System.out.println("Ошибка: Видео не найдено!");
            return;
        }

        Mat frame = new Mat();
        Mat gray = new Mat();
        Mat edges = new Mat();
        Mat lines = new Mat();

        // Создаем окно
        ImageViewer window = new ImageViewer("Lane Detection (Bottom 40%)");

        while (capture.read(frame)) {
            if (frame.empty()) break;

            // 1. Стандартная обработка (ч/б -> размытие -> края)
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(5, 5), 0);
            Imgproc.Canny(gray, edges, 50, 150);

            // 2. ОБРЕЗКА: Оставляем только нижние 40% кадра
            Mat maskedEdges = new Mat();
            maskBottom40Percent(edges, maskedEdges);

            // 3. Поиск линий
            // minLineLength уменьшил до 30, так как мы смотрим на короткий участок
            Imgproc.HoughLinesP(maskedEdges, lines, 1, Math.PI / 180, 50, 30, 10);

            // --- ЛОГИКА НАПРАВЛЕНИЯ ---
            double sumX = 0;
            int count = 0;

            for (int i = 0; i < lines.rows(); i++) {
                double[] l = lines.get(i, 0);
                // Рисуем линии зеленым на оригинале
                Imgproc.line(frame, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 255, 0), 2);

                // Ищем верхнюю точку линии (наименьший Y) в нашей нижней зоне
                double topX = (l[1] < l[3]) ? l[0] : l[2];
                sumX += topX;
                count++;
            }

            // Координаты для стрелки (теперь она ниже)
            double centerX = frame.width() / 2.0;
            double arrowY = frame.height() * 0.8; // Стрелка на высоте 80% от верха

            Point arrowStart = new Point(centerX, arrowY + 50);
            Point arrowEnd = new Point(centerX, arrowY - 50); // По умолчанию прямо

            String textDirection = "STRAIGHT";

            if (count > 0) {
                double avgTopX = sumX / count;

                // Чувствительность поворота
                double threshold = 40.0;

                if (avgTopX < centerX - threshold) {
                    textDirection = "LEFT";
                    // Стрелка наклоняется влево
                    arrowEnd = new Point(centerX - 80, arrowY - 20);
                } else if (avgTopX > centerX + threshold) {
                    textDirection = "RIGHT";
                    // Стрелка наклоняется вправо
                    arrowEnd = new Point(centerX + 80, arrowY - 20);
                }
            }

            // Рисуем жирную красную стрелку
            Imgproc.arrowedLine(frame, arrowStart, arrowEnd, new Scalar(0, 0, 255), 10, 8, 0, 0.4);

            // Пишем текст
            Imgproc.putText(frame, textDirection, new Point(50, frame.height() - 50),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.5, new Scalar(0, 255, 255), 3);

            // Показываем результат
            window.showImage(frame);

            // Освобождаем память
            maskedEdges.release();

            try { Thread.sleep(30); } catch (InterruptedException e) {}
        }
        capture.release();
        System.exit(0);
    }

    // --- НОВАЯ ФУНКЦИЯ ОБРЕЗКИ (ТРАПЕЦИЯ) ---
    private static void maskBottom40Percent(Mat source, Mat destination) {
        Mat mask = Mat.zeros(source.size(), source.type());

        int w = source.width();
        int h = source.height();

        // Рассчитываем высоту среза (оставляем только нижние 40%)
        // h * 0.60 означает, что мы начинаем с 60-го процента высоты и идем вниз
        int topY = (int) (h * 0.60);

        // Определяем 4 точки трапеции
        Point p1 = new Point(0, h);              // Левый нижний угол
        Point p2 = new Point(w, h);              // Правый нижний угол
        Point p3 = new Point(w * 0.90, topY);    // Правый верхний (чуть уже края)
        Point p4 = new Point(w * 0.10, topY);    // Левый верхний (чуть уже края)

        MatOfPoint polygon = new MatOfPoint(p1, p2, p3, p4);
        List<MatOfPoint> list = new ArrayList<>();
        list.add(polygon);

        // Заливаем трапецию белым
        Imgproc.fillPoly(mask, list, new Scalar(255));

        // Накладываем маску
        Core.bitwise_and(source, mask, destination);

        mask.release();
        polygon.release();
    }

    // --- Окно просмотра ---
    public static class ImageViewer {
        private final JFrame frame;
        private final JLabel imageLabel;
        public ImageViewer(String title) {
            frame = new JFrame(title);
            imageLabel = new JLabel();
            frame.add(imageLabel);
            frame.setSize(800, 600);
            frame.setVisible(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        public void showImage(Mat mat) {
            int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
            int bufferSize = mat.channels() * mat.cols() * mat.rows();
            byte[] b = new byte[bufferSize];
            mat.get(0, 0, b);
            BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
            final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(b, 0, targetPixels, 0, b.length);
            imageLabel.setIcon(new ImageIcon(image));
            frame.pack();
        }
    }
}