package smartpark.service;

import smartpark.model.Ticket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Background daemon thread that monitors active parking tickets for overstays.
 * Demonstrates:
 * 1. Java Daemon threads (Thread.setDaemon(true)) that terminate automatically when user threads exit.
 * 2. Volatile keyword for thread-safe cross-CPU cache visibility of shutdown flags.
 */
public class OverstayWatcher implements Runnable {
    private final ParkingLot parkingLot;
    private final long maxStayMillis;
    private final long pollIntervalMillis;
    private volatile boolean running;
    private final List<Ticket> flaggedOverstays;

    public OverstayWatcher(ParkingLot parkingLot, long maxStayMillis, long pollIntervalMillis) {
        this.parkingLot = parkingLot;
        this.maxStayMillis = maxStayMillis;
        this.pollIntervalMillis = pollIntervalMillis;
        this.running = false;
        this.flaggedOverstays = new ArrayList<>();
    }

    public List<Ticket> getFlaggedOverstays() {
        synchronized (flaggedOverstays) {
            return Collections.unmodifiableList(new ArrayList<>(flaggedOverstays));
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        this.running = true;
        while (running) {
            try {
                long now = System.currentTimeMillis();
                List<Ticket> active = parkingLot.getActiveTickets();

                synchronized (flaggedOverstays) {
                    flaggedOverstays.clear();
                    for (Ticket ticket : active) {
                        long duration = now - ticket.getEntryTimeMillis();
                        if (duration > maxStayMillis) {
                            flaggedOverstays.add(ticket);
                        }
                    }
                }

                Thread.sleep(pollIntervalMillis);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Safeguard against unexpected runtime exceptions in daemon loop
                System.err.println("[DAEMON WARN] OverstayWatcher cycle error: " + e.getMessage());
            }
        }
    }
}
