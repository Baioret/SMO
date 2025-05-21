package org.baioret;

public class Uniform {
    public static double generate(double a, double b) {
        return Math.random() * (b - a) + a;
    }
}
