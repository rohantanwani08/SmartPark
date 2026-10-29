package smartpark.service;

import smartpark.model.Ticket;
import smartpark.model.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simulates an independent physical entry gate operating concurrently on its own thread.
 * Demonstrates:
 * 1. Java Runnable interface for multithreading.
 * 2. Thread-safe concurrent access to the shared ParkingLot monitor.
 */
public class Gate implements Runnable {
    private final String gateName;
    private final ParkingLot parkingLot;
    private final List<Vehicle> incomingQueue;
    private final List<Ticket> issuedTickets;
    private final List<String> errorLog;

    public Gate(String gateName, ParkingLot parkingLot, List<Vehicle> incomingQueue) {
        this.gateName = gateName;
        this.parkingLot = parkingLot;
        this.incomingQueue = incomingQueue != null ? incomingQueue : new ArrayList<>();
        this.issuedTickets = new ArrayList<>();
        this.errorLog = new ArrayList<>();
    }

    public String getGateName() {
        return gateName;
    }

    public List<Ticket> getIssuedTickets() {
        return Collections.unmodifiableList(issuedTickets);
    }

    public List<String> getErrorLog() {
        return Collections.unmodifiableList(errorLog);
    }

    @Override
    public void run() {
        for (Vehicle vehicle : incomingQueue) {
            try {
                // Concurrently attempt admission using wait/notify coordination
                Ticket ticket = parkingLot.parkVehicleWait(vehicle);
                issuedTickets.add(ticket);
                // Simulate physical sensor and barrier delay
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                errorLog.add("Gate thread interrupted: " + e.getMessage());
                break;
            } catch (Exception e) {
                errorLog.add("Admission failed for " + vehicle.getPlateNumber() + ": " + e.getMessage());
            }
        }
    }
}
