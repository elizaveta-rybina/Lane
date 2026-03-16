package org.example;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Точка входа приложения для трекинга дорожных полос на видео.
 * 
 * Использует систему Lane Departure Warning (LDW) для предупреждения о выезде с полосы.
 */
public class Main {

    private static final String VIDEO_DIR = "video";
    private static final String DEFAULT_VIDEO_NAME = "road.mp4";
    private static final double SMOOTHING_ALPHA = 0.2;
    private static final double DEPARTURE_THRESHOLD = 0.3; // метры

    /**
     * Запуск приложения.
     *
     * @param args args[0] — необязательный путь к видеофайлу или имя файла из папки video
     */
    public static void main(String[] args) {
        OpenCV.loadLocally();

        String videoPath = resolveVideoPath(args);
        VideoCapture capture = new VideoCapture(videoPath);

        if (!capture.isOpened()) {
            System.out.println("Ошибка: видео не найдено или не открывается: " + videoPath);
            return;
        }

        Mat frame = new Mat();
        ImageViewer window = new ImageViewer("Lane Departure Warning System");

        // Инициализируем конфигурацию автомобиля и систему LDW
        VehicleConfig vehicleConfig = new VehicleConfig();
        System.out.println("Конфигурация автомобиля: " + vehicleConfig);
        System.out.println("Порог предупреждения: " + DEPARTURE_THRESHOLD + " м");

        LanePipeline pipeline = new LanePipeline(SMOOTHING_ALPHA, vehicleConfig, DEPARTURE_THRESHOLD);
        LaneRenderer renderer = new LaneRenderer();

        int frameCount = 0;
        long startTime = System.currentTimeMillis();

        try {
            while (capture.read(frame)) {
                if (frame.empty()) {
                    break;
                }

                frameCount++;

                LaneEstimate estimate = pipeline.process(frame);
                renderer.render(frame, estimate, pipeline.getLDW());

                // Логирование предупреждений
                if (pipeline.isDeparture()) {
                    System.out.printf("[Frame %d] LANE DEPARTURE DETECTED - " +
                                    "Left: %.2f m, Right: %.2f m, Min: %.2f m%n",
                            frameCount,
                            pipeline.getLDW().getLeftDistance(),
                            pipeline.getLDW().getRightDistance(),
                            pipeline.getMinDistance());
                }

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
            double fps = frameCount / elapsedSeconds;
            System.out.printf("Обработано %d кадров за %.2f сек (%.1f FPS)%n", frameCount, elapsedSeconds, fps);
        }

        System.exit(0);
    }

    private static String resolveVideoPath(String[] args) {
        if (args.length == 0) {
            return Paths.get(VIDEO_DIR, DEFAULT_VIDEO_NAME).toString();
        }

        Path rawPath = Paths.get(args[0]);
        if (Files.exists(rawPath)) {
            return rawPath.toString();
        }

        return Paths.get(VIDEO_DIR, args[0]).toString();
    }
}