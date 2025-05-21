package org.baioret;

import java.util.*;

public class SmoSimulator {
    private final double start;
    private final double finish;
    private final double lambda;

    private final List<Client> clients = new ArrayList<>();
    private final List<Integer> queueSizes = new ArrayList<>();
    private final List<String[]> eventLog = new ArrayList<>();

    private final PoissonGenerator poissonGenerator;
    private final Distribution serviceDistribution;

    private double tA, tD, t, tP, idleTime;
    private int n, NA, ND;
    private final PriorityQueue<Client> queue = new PriorityQueue<>(Comparator.comparingDouble(c -> c.arrivalTime));

    public SmoSimulator(double start, double finish, double lambda, double serviceLambda) {
        this.start = start;
        this.finish = finish;
        this.lambda = lambda;
        this.poissonGenerator = new PoissonGenerator(lambda);
        this.serviceDistribution = new Distribution(serviceLambda);
    }

    public void run() {
        t = start;
        tA = poissonGenerator.generateNextArrival(t);
        tD = Double.POSITIVE_INFINITY;

        while (true) {
            if (tA <= tD && tA <= finish) {
                handleArrival();
            } else if (tD <= tA && tD <= finish) {
                handleDeparture();
            } else if (Math.min(tA, tD) > finish && n > 0) {
                handlePostCloseDeparture();
            } else {
                double lastDepartureTime = clients.stream()
                        .mapToDouble(c -> c.departureTime)
                        .max()
                        .orElse(finish);
                tP = Math.max(lastDepartureTime - finish, 0);
                break;
            }
        }
    }

    private void handleArrival() {
        t = tA;
        NA++;
        n++;
        queueSizes.add(n);

        Client client = new Client();
        client.arrivalTime = t;
        clients.add(client);
        tA = poissonGenerator.generateNextArrival(t);

        if (n == 1) {
            client.serviceStartTime = t;
            double serviceTime = serviceDistribution.generateServiceTime();
            client.departureTime = t + serviceTime;
            tD = client.departureTime;
        }

        queue.add(client);
        eventLog.add(new String[] {
                "Клиент " + NA + " пришел",
                String.format("%.5f", t),
                String.valueOf(n)
        });
    }

    private void handleDeparture() {
        t = tD;
        ND++;
        n--;
        queueSizes.add(n);

        Client client = queue.poll();
        if (client != null) {
            if (client.serviceStartTime == client.arrivalTime && ND == 1) {
                idleTime += client.arrivalTime - start;
            } else if (client.getWaitingTime() == 0 && ND > 1) {
                idleTime += client.arrivalTime - clients.get(ND - 2).departureTime;
            }
        }

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

    private void handlePostCloseDeparture() {
        t = tD;
        ND++;
        n--;
        queueSizes.add(n);

        Client client = queue.poll();
        if (client != null) {
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

    public void printStatistics() {
        printEventLog();
        printClientStatsTable();

        double avgW = clients.stream().mapToDouble(Client::getWaitingTime).average().orElse(0.0);
        double avgS = clients.stream().mapToDouble(Client::getSystemTime).average().orElse(0.0);
        double avgQ = queueSizes.stream().mapToInt(i -> i).average().orElse(0.0);
        double ro = ((finish - start) - idleTime) / (finish - start);

        System.out.println("---СТАТИСТИКА---");
        System.out.println("Всего пришедших клиентов: " + clients.size());
        System.out.printf("Задержка последнего клиента: %.5f\n", tP);
        System.out.printf("Среднее время ожидания: %.5f\n", avgW);
        System.out.printf("Средняя длина очереди: %.2f\n", avgQ);
        System.out.printf("Среднее время клиента в системе: %.5f\n", avgS);
        System.out.printf("Оценка занятости устройства: %.5f\n\n", ro);
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
