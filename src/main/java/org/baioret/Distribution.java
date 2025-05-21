package org.baioret;

public class Distribution {
    private final double lambda;

    public Distribution(double lambda) {
        this.lambda = lambda;
    }

    public double generateServiceTime() {
        return -Math.log(1 - Uniform.generate(0.0, 1.0) / lambda * 2);
    }
}