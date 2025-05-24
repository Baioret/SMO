package org.baioret;

// З-н распр-я - как часто различные значения СВ встречаются в наборе данных
// Ф-ция распр-я - вер-ть того, что СВ не превышает Х
// Возможное значение - значение, принадлежащее данному з-ну распр-я
// Ф-ция плотности - вер-ть попадания СВ в интервал
/**
 * Генератор прибытия клиентов по неоднородному Пуассоновскому процессу.
 * ПП - математическая модель потока случайных событий, несвязанных между собой, происходящих с постоянной/переменной интенсивностью.
 * В данном случае ПП неоднородный - N(0) = 0, ординарный, нестационарный (интенсивность задана функцией), без последействия
 */
public class PoissonGenerator {
    private final double lambdaMax;

    public PoissonGenerator(double lambdaMax) {
        this.lambdaMax = lambdaMax;
    }

    /**
     * Генерация следующего времени прибытия клиента
     * @param currentTime текущее модельное время
     * @return время следующего события
     */
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