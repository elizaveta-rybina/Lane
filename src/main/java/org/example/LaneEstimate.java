package org.example;

/**
 * Результат оценки положения полос на одном кадре.
 *
 * @param leftTopX   X-координата верхней точки левой линии
 * @param leftSlope  наклон левой линии
 * @param rightTopX  X-координата верхней точки правой линии
 * @param rightSlope наклон правой линии
 * @param frameWidth ширина кадра
 * @param frameHeight высота кадра
 */
public record LaneEstimate(
        Double leftTopX,
        Double leftSlope,
        Double rightTopX,
        Double rightSlope,
        double frameWidth,
        double frameHeight
) {
}
