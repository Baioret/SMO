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
                "Client " + NA + " came",
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
                    "Client " + ND + " left",
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
                    "Client " + ND + " left",
                    String.format("%.5f", client.departureTime),
                    String.valueOf(n)
            });
        }
    }

    private void verifyLambdaRatio() {
        System.out.println("Let's be sure that 0 <= λ(t)/λ_max <= 1");
        boolean allOk = true;
        for (double t = start; t <= finish; t += 0.5) {
            double lambdaValue = IntensityFunction.lambda(t);
            double ratio = lambdaValue / lambda;

            if (ratio < 0 || ratio > 1) {
                allOk = false;
                System.out.printf("❌ Invalid at t = %.2f: λ(t)/λ_max = %.3f\n", t, ratio);
            }
        }
        if (allOk) {
            System.out.println("✅ All values of λ(t)/λ_max are within [0, 1]");
        }
    }

    public void printStatistics() {
        printEventLog();
        printClientStatsTable();

        double avgW = clients.stream().mapToDouble(Client::getWaitingTime).average().orElse(0.0);
        double avgS = clients.stream().mapToDouble(Client::getSystemTime).average().orElse(0.0);
        double avgQ = queueSizes.stream().mapToInt(i -> i).average().orElse(0.0);
        double ro = ((finish - start) - idleTime) / (finish - start);

        System.out.println("Statistics:");
        System.out.println("The total number of clients who came: " + clients.size());
        System.out.printf("Delay time: %.5f\n", tP);
        System.out.printf("Average time of clients in the queue: %.5f\n", avgW);
        System.out.printf("Average queue length: %.2f\n", avgQ);
        System.out.printf("Average client time in the system: %.5f\n", avgS);
        System.out.printf("Device occupancy rate: %.5f\n\n", ro);
        verifyLambdaRatio();
    }

    private void printEventLog() {
        printTable(new String[] { "Event", "Time", "Clients in system" }, eventLog);
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
                "#", "Time of arrival", "Time of starting the service",
                "Time of waiting", "Time of service", "Time of leaving", "Time in the system"
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
