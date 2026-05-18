package org.example;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    private static final String VIDEO_DIR = "video";
    private static final String DEFAULT_VIDEO_NAME = "V1_regular.mp4";
    private static final double SMOOTHING_ALPHA = 0.2;
    private static final double DEPARTURE_THRESHOLD = 0.3; // метры

    public static void main(String[] args) {
        OpenCV.loadLocally();

        String videoPath = resolveVideoPath(args);
        VideoCapture capture = new VideoCapture(videoPath);

        if (!capture.isOpened()) {
            System.out.println("Ошибка: видео не найдено или не открывается: " + videoPath);
            return;
        }

        Mat frame = new Mat();
        ImageViewer window = new ImageViewer("Интерфейс калибровки LDW");

        VehicleConfig vehicleConfig = new VehicleConfig();
        LanePipeline pipeline = new LanePipeline(SMOOTHING_ALPHA, vehicleConfig, DEPARTURE_THRESHOLD);
        LaneRenderer renderer = new LaneRenderer();

        int frameCount = 0;
        long startTime = System.currentTimeMillis();

        try {
            while (capture.read(frame)) {
                if (frame.empty()) break;
                frameCount++;

                // Считываем значения с интерфейса и передаем в алгоритм
                pipeline.updateCalibration(window.getTopY(), window.getTopWidth(), window.getBottomWidth());

                LaneEstimate estimate = pipeline.process(frame);
                renderer.render(frame, estimate, pipeline.getLDW());

                window.showImage(frame);

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            frame.release();
            pipeline.release();
            capture.release();

            long endTime = System.currentTimeMillis();
            double elapsedSeconds = (endTime - startTime) / 1000.0;
            System.out.printf("Обработано %d кадров за %.2f сек (%.1f FPS)%n", frameCount, elapsedSeconds, frameCount / elapsedSeconds);
        }
        System.exit(0);
    }

    private static String resolveVideoPath(String[] args) {
        if (args.length == 0) return Paths.get(VIDEO_DIR, DEFAULT_VIDEO_NAME).toString();
        Path rawPath = Paths.get(args[0]);
        if (Files.exists(rawPath)) return rawPath.toString();
        return Paths.get(VIDEO_DIR, args[0]).toString();
    }
}