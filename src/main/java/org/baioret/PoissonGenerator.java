package org.baioret;

public class PoissonGenerator {
    private final double lambdaMax;

    public PoissonGenerator(double lambdaMax) {
        this.lambdaMax = lambdaMax;
    }

    public double generateNextArrival(double currentTime) {
        double t = currentTime;
        while (true) {
            double u1 = Uniform.generate(0.0, 1.0);
            t -= Math.log(u1) / lambdaMax;
            double u2 = Uniform.generate(0.0, 1.0);
            if (u2 <= IntensityFunction.lambda(t) / lambdaMax) {
                return t;
            }
        }
    }
}