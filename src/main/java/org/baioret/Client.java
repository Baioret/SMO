package org.baioret;

public class Client {
    public double arrivalTime;
    public double serviceStartTime;
    public double departureTime;

    public double getWaitingTime() {
        return Math.max(0, serviceStartTime - arrivalTime);
    }

    public double getServiceTime() {
        return departureTime - serviceStartTime;
    }

    public double getSystemTime() {
        return departureTime - arrivalTime;
    }
}

