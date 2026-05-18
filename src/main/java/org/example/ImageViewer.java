package org.example;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Swing-окно для отображения видео и калибровки системы Bird's-Eye View.
 */
public class ImageViewer {
    private final JFrame frame;
    private final JLabel imageLabel;
    
    // Переменные для калибровки (хранят значения в процентах / 100)
    private double topY = 0.65;
    private double topWidth = 0.10;
    private double bottomWidth = 0.80;

    public ImageViewer(String title) {
        frame = new JFrame(title);
        frame.setLayout(new BorderLayout());

        imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        frame.add(imageLabel, BorderLayout.CENTER);

        // --- ПАНЕЛЬ УПРАВЛЕНИЯ ---
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(3, 2, 10, 5));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Горизонт (Top Y)
        JSlider topYSlider = new JSlider(40, 90, 65); // от 0.40 до 0.90
        topYSlider.addChangeListener(e -> topY = topYSlider.getValue() / 100.0);
        controlPanel.add(new JLabel("Высота горизонта (Top Y):"));
        controlPanel.add(topYSlider);

        // 2. Ширина верха трапеции
        JSlider topWidthSlider = new JSlider(5, 50, 10); // от 0.05 до 0.50
        topWidthSlider.addChangeListener(e -> topWidth = topWidthSlider.getValue() / 100.0);
        controlPanel.add(new JLabel("Ширина вдали (Top Width):"));
        controlPanel.add(topWidthSlider);

        // 3. Ширина низа трапеции (возле капота)
        JSlider bottomWidthSlider = new JSlider(40, 100, 80); // от 0.40 до 1.00
        bottomWidthSlider.addChangeListener(e -> bottomWidth = bottomWidthSlider.getValue() / 100.0);
        controlPanel.add(new JLabel("Ширина у капота (Bottom Width):"));
        controlPanel.add(bottomWidthSlider);

        frame.add(controlPanel, BorderLayout.SOUTH);

        frame.setSize(1024, 768);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    // Геттеры для передачи в Pipeline
    public double getTopY() { return topY; }
    public double getTopWidth() { return topWidth; }
    public double getBottomWidth() { return bottomWidth; }

    public void showImage(Mat mat) {
        int type = (mat.channels() > 1) ? BufferedImage.TYPE_3BYTE_BGR : BufferedImage.TYPE_BYTE_GRAY;
        int bufferSize = mat.channels() * mat.cols() * mat.rows();
        byte[] b = new byte[bufferSize];
        mat.get(0, 0, b);
        BufferedImage image = new BufferedImage(mat.cols(), mat.rows(), type);
        final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(b, 0, targetPixels, 0, b.length);

        imageLabel.setIcon(new ImageIcon(image));
        // Убрали frame.pack(), чтобы окно не скакало при рендере
    }
}