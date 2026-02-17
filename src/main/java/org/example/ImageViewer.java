package org.example;

import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Простое Swing-окно для покадрового отображения Mat из OpenCV.
 */
public class ImageViewer {
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

    /**
     * Показывает кадр OpenCV в окне.
     *
     * @param mat исходный кадр в формате OpenCV Mat
     */
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