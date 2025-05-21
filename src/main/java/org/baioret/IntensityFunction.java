package org.baioret;

public class IntensityFunction {
    public static double lambda(double t) {
        if (t >= 7 && t < 9) return 1.4 * t - 9.2;
        if (t >= 9 && t < 11) return -0.23 * t + 4.3;
        if (t >= 11 && t < 13) return -0.2 * t + 4.4;
        if (t >= 13 && t < 15) return -0.8 * (t - 14) * (t - 14) + 5.8;
        if (t >= 15 && t < 17) return -0.2 * t + 6.1;
        if (t >= 17 && t < 19) return 0.6 * t - 9.0;
        if (t >= 19 && t < 21) return -1.5 * (t - 20) * (t - 20) + 5.5;
        if (t >= 21 && t < 23) return -0.8 * t + 19.65;
        return 0.01;
    }
}