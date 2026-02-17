package org.example;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Точка входа приложения для трекинга дорожных полос на видео.
 */
public class Main {

    private static final String VIDEO_DIR = "video";
    private static final String DEFAULT_VIDEO_NAME = "road.mp4";
    private static final double SMOOTHING_ALPHA = 0.2;

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
        ImageViewer window = new ImageViewer("Smooth Lane Tracker");

        LanePipeline pipeline = new LanePipeline(SMOOTHING_ALPHA);
        LaneRenderer renderer = new LaneRenderer();

        try {
            while (capture.read(frame)) {
                if (frame.empty()) {
                    break;
                }

                LaneEstimate estimate = pipeline.process(frame);
                renderer.render(frame, estimate);
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