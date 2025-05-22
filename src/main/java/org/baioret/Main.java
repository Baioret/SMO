package org.baioret;

public class Main {
    public static void main(String[] args) {
        SmoSimulator simulator = new SmoSimulator(7.0, 23.0, 5.8, 5.0);
        simulator.run();
        simulator.printStatistics();
        simulator.showQueueChart();
    }
}