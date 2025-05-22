package org.baioret;

import java.util.*;

/**
 * Класс SmoSimulator моделирует работу СМО с одним устройством
 * с помощью генерации событий прихода и ухода клиентов. Использует нестационарный поток и
 * экспоненциальное распределение времени обслуживания.
 */
public class SmoSimulator {

    private final boolean testMode = false; // переключатель
    private final int maxClients = 6;     // максимум клиентов в режиме теста

    // Временные границы моделирования
    private final double start;
    private final double finish;

    // Максимальная интенсивность входящего потока λ(t) <= lambda
    private final double lambda;

    // Список клиентов и логирование
    private final List<Client> clients = new ArrayList<>();
    private final List<Integer> queueSizes = new ArrayList<>();
    private final List<String[]> eventLog = new ArrayList<>();

    // Генераторы событий
    private final PoissonGenerator poissonGenerator;
    private final Distribution serviceDistribution;

    // Текущие времена событий и счётчики
    /*
     * Временные переменные:
     * tA — время следующего прихода клиента
     * tD — время следующего ухода клиента
     * t — текущее модельное время
     * tP — задержка (время, на которое последний клиент задерживает систему после закрытия)
     * idleTime — накопленное время простоя устройства (когда оно не было занято обслуживанием)
     */
    private double tA, tD, t, tP, idleTime; //
    private int n, NA, ND; // n — клиентов в системе, NA — пришло, ND — ушло

    // Очередь обслуживаемых клиентов
    private final PriorityQueue<Client> queue = new PriorityQueue<>(Comparator.comparingDouble(c -> c.arrivalTime));

    /**
     * Конструктор симулятора
     * @param start время начала моделирования
     * @param finish время окончания (закрытие системы)
     * @param lambda максимальная интенсивность λ(t)
     * @param serviceLambda интенсивность обслуживания (μ)
     */
    public SmoSimulator(double start, double finish, double lambda, double serviceLambda) {
        this.start = start;
        this.finish = finish;
        this.lambda = lambda;
        this.poissonGenerator = new PoissonGenerator(lambda);
        this.serviceDistribution = new Distribution(serviceLambda);
    }

    /**
     * Основной метод запуска моделирования.
     * Управляет обработкой событий до завершения работы системы.
     */
    public void run() {
        t = start;
        tA = poissonGenerator.generateNextArrival(t);
        tD = Double.POSITIVE_INFINITY;

        while (true) {
            // Приход клиента
            if (tA <= tD && tA <= finish && (!testMode || NA < maxClients)) {
                handleArrival();
            }
            // Уход клиента
            else if (tD <= tA && tD <= finish) {
                handleDeparture();
            }
            // После закрытия — обслуживаем оставшихся
            else if (Math.min(tA, tD) > finish && n > 0) {
                handlePostCloseDeparture();
            }
            // Завершение моделирования — считаем задержку
            else {
                double lastDepartureTime = clients.stream()
                        .mapToDouble(c -> c.departureTime)
                        .max()
                        .orElse(finish);
                tP = Math.max(lastDepartureTime - finish, 0);
                break;
            }
        }
    }

    /**
     * Обработка события прихода клиента
     */
    private void handleArrival() {
        t = tA;
        NA++;
        n++;
        queueSizes.add(n);

        // Создаём нового клиента
        Client client = new Client();
        client.arrivalTime = t;
        clients.add(client);

        // Генерируем следующее время прихода
        tA = poissonGenerator.generateNextArrival(t);

        // Если клиент обслуживается сразу
        if (n == 1) {
            client.serviceStartTime = t;
            double serviceTime = serviceDistribution.generateServiceTime();
            client.departureTime = t + serviceTime;
            tD = client.departureTime;
        }

        queue.add(client);

        // Логируем событие
        eventLog.add(new String[] {
                "Клиент " + NA + " пришел",
                String.format("%.5f", t),
                String.valueOf(n)
        });
    }

    /**
     * Обработка события ухода клиента
     */
    private void handleDeparture() {
        t = tD;
        ND++;
        n--;
        queueSizes.add(n);

        // Обработка завершившего обслуживание клиента
        Client client = queue.poll();
        if (client != null) {
            // Учёт времени простоя
            if (client.serviceStartTime == client.arrivalTime && ND == 1) {
                idleTime += client.arrivalTime - start;
            } else if (client.getWaitingTime() == 0 && ND > 1) {
                idleTime += client.arrivalTime - clients.get(ND - 2).departureTime;
            }
        }

        // Назначаем обслуживание следующему
        if (n == 0) {
            tD = Double.POSITIVE_INFINITY;
        } else {
            Client next = queue.peek();
            if (next != null) {
                next.serviceStartTime = t;
                double serviceTime = serviceDistribution.generateServiceTime();
                next.departureTime = t + serviceTime;
                tD = next.departureTime;
            }
        }

        if (client != null) {
            eventLog.add(new String[] {
                    "Клиент " + ND + " ушел",
                    String.format("%.5f", client.departureTime),
                    String.valueOf(n)
            });
        }
    }

    /**
     * Обслуживание оставшихся клиентов после закрытия
     */
    private void handlePostCloseDeparture() {
        t = tD;
        ND++;
        n--;
        queueSizes.add(n);

        Client client = queue.poll();
        if (client != null) {
            // Учёт времени простоя после закрытия
            if (client.getWaitingTime() == 0 && ND == 1) {
                idleTime += client.arrivalTime - start;
            } else if (client.getWaitingTime() == 0 && ND > 1) {
                idleTime += client.arrivalTime - clients.get(ND - 2).departureTime;
            }

            client.serviceStartTime = t;
            double serviceTime = serviceDistribution.generateServiceTime();
            client.departureTime = t + serviceTime;
            tD = client.departureTime;

            eventLog.add(new String[] {
                    "Клиент " + ND + " ушел",
                    String.format("%.5f", client.departureTime),
                    String.valueOf(n)
            });
        }
    }

    /**
     * Проверка допустимости значений λ(t)/λ_max ∈ [0, 1]
     */
    private void verifyLambdaRatio() {
        System.out.println("Убедимся, что все 0 <= λ(t)/λ_max <= 1");
        boolean allOk = true;
        for (double t = start; t <= finish; t += 0.5) {
            double lambdaValue = IntensityFunction.lambda(t);
            double ratio = lambdaValue / lambda;

            if (ratio < 0 || ratio > 1) {
                allOk = false;
                System.out.printf("❌ Неверно на t = %.2f: λ(t)/λ_max = %.3f\n", t, ratio);
            }
        }
        if (allOk) {
            System.out.println("✅ Все значения λ(t)/λ_max принадлежат [0, 1]");
        }
    }

    /**
     * Вывод статистики и логов событий
     */
    public void printStatistics() {
        printEventLog();
        printClientStatsTable();

        double avgW = clients.stream().mapToDouble(Client::getWaitingTime).average().orElse(0.0);
        double avgS = clients.stream().mapToDouble(Client::getSystemTime).average().orElse(0.0);
        double avgQ = queueSizes.stream().mapToInt(i -> i).average().orElse(0.0);
        double ro = ((finish - start) - idleTime) / (finish - start);

        System.out.println("---СТАТИСТИКИ---");
        System.out.println("Всего пришедших клиентов: " + clients.size());
        System.out.printf("Задержка последнего клиента: %.5f\n", tP);
        System.out.printf("Среднее время ожидания: %.5f\n", avgW);
        System.out.printf("Средняя длина очереди: %.2f\n", avgQ);
        System.out.printf("Среднее время клиента в системе: %.5f\n", avgS);
        System.out.printf("Оценка занятости устройства: %.5f\n\n", ro);

        // Проверка отношения λ(t)/λ_max
        verifyLambdaRatio();
    }

    private void printEventLog() {
        printTable(new String[] { "Событие", "Время", "Очередь" }, eventLog);
    }

    private void printClientStatsTable() {
        List<String[]> rows = new ArrayList<>();
        for (int i = 0; i < clients.size(); i++) {
            Client c = clients.get(i);
            rows.add(new String[] {
                    String.valueOf(i + 1),
                    String.format("%.5f", c.arrivalTime),
                    String.format("%.5f", c.serviceStartTime),
                    String.format("%.5f", c.getWaitingTime()),
                    String.format("%.5f", c.getServiceTime()),
                    String.format("%.5f", c.departureTime),
                    String.format("%.5f", c.getSystemTime())
            });
        }

        printTable(new String[] {
                "#", "Время прихода", "Время начала обслуживания",
                "Время ожидания", "Время обслуживания", "Время ухода", "Время в системе"
        }, rows);
    }

    private void printTable(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder separator = new StringBuilder("+");
        StringBuilder formatBuilder = new StringBuilder();
        for (int w : widths) {
            separator.append("-".repeat(w + 2)).append("+");
            formatBuilder.append("| %-").append(w).append("s ");
        }
        formatBuilder.append("|\n");
        String format = formatBuilder.toString();

        System.out.println(separator);
        System.out.printf(format, (Object[]) headers);
        System.out.println(separator);
        for (String[] row : rows) {
            System.out.printf(format, (Object[]) row);
        }
        System.out.println(separator);
    }
}
