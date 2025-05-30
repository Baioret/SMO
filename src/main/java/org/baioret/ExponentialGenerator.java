package org.baioret;

/**
 * Генератор времени обслуживания по экспоненциальному распределению
 */
public class ExponentialGenerator {
    private final double lambda;

    public ExponentialGenerator(double lambda) {
        this.lambda = lambda;
    }

    /**
     * Генерация времени обслуживания клиента по экспоненциальному закону с параметром λ
     * @return время обслуживания клиента
     */
    public double generateServiceTime() {
        return -Math.log(1 - Uniform.generate(0.0, 1.0) / lambda * 2);
    }
}