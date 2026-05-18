package org.example;

import org.opencv.core.Mat;

/**
 * Результат оценки положения полос на одном кадре.
 *
 * @param leftPoly   коэффициенты полинома левой линии (A, B, C)
 * @param rightPoly  коэффициенты полинома правой линии (A, B, C)
 * @param invM       Матрица обратного перспективного преобразования для рендера
 * @param frameWidth ширина кадра
 * @param frameHeight высота кадра
 */
public record LaneEstimate(
        double[] leftPoly,
        double[] rightPoly,
        Mat invM,
        double frameWidth,
        double frameHeight
) {
}