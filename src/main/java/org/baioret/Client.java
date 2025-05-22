package org.baioret;

/**
 * Класс клиента в СМО. Хранит времена прибытия, начала обслуживания и ухода
 */
public class Client {
    public double arrivalTime;         // Время прибытия клиента
    public double serviceStartTime;    // Время начала обслуживания
    public double departureTime;       // Время ухода клиента

    /**
     * Возвращает время ожидания клиента в очереди
     */
    public double getWaitingTime() {
        return Math.max(0, serviceStartTime - arrivalTime);
    }

    /**
     * Возвращает длительность обслуживания клиента
     */
    public double getServiceTime() {
        return departureTime - serviceStartTime;
    }

    /**
     * Возвращает общее время клиента в системе
     */
    public double getSystemTime() {
        return departureTime - arrivalTime;
    }
}

