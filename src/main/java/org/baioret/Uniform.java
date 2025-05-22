package org.baioret;

/**
 * Вспомогательный класс для генерации случайных чисел по равномерному распределению
 */
public class Uniform {

    /**
     * Возвращает случайное число из диапазона [min, max) по равномерному распределению
     */
    public static double generate(double a, double b) {
        return Math.random() * (b - a) + a;
    }
}
