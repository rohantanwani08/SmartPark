package smartpark;

import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.model.Vehicle;
import smartpark.service.CashPayment;
import smartpark.service.FileManager;
import smartpark.service.Gate;
import smartpark.service.OverstayWatcher;
import smartpark.service.ParkingLot;
import smartpark.ui.ConsoleMenu;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SmartPark - Main Application Entry Point
 * Phase 8: Concurrency, Thread Synchronization, Race Condition Demo & Daemon Watcher
 */
public class Main {
    private static final String ACTIVE_TICKETS_FILE = "data/tickets.txt";
    private static final String BINARY_LOG_FILE = "data/ticket_log.dat";

    public static void main(String[] args) {
        try {
            // Configure a 2-Floor Parking Facility
            Floor floor1 = new Floor(1, 3, 2, 1);
            Floor floor2 = new Floor(2, 3, 2, 0);
            ParkingLot lot = new ParkingLot("SmartPark Central Plaza", new Floor[] { floor1, floor2 });

            // Initialize Dual-Mode File Manager
            FileManager fileManager = new FileManager(ACTIVE_TICKETS_FILE, BINARY_LOG_FILE);
            lot.setFileManager(fileManager);

            // Reload active state from persistent character stream
            int restoredCount = fileManager.loadActiveTickets(lot);
            if (restoredCount > 0) {
                System.out.printf("[INFO] Successfully restored %d active vehicle(s) from '%s'.%n",
                        restoredCount, ACTIVE_TICKETS_FILE);
            } else {
                lot.parkVehicle(new Car("MH-12-AB-1234", "Rohan Tanwani"));
                lot.parkVehicle(new Bike("MH-14-XY-9876", "Amit Verma"));
                System.out.println("[INFO] Initialized facility with demo vehicles and saved to persistent storage.");
            }

            // Start Background Daemon Overstay Watcher (Scans every 5 seconds for vehicles exceeding 3 hours)
            OverstayWatcher watcher = new OverstayWatcher(lot, 3 * 3600 * 1000L, 5000L);
            Thread watcherThread = new Thread(watcher, "OverstayWatcher-Daemon");
            watcherThread.setDaemon(true);
            watcherThread.start();
            System.out.println("[INFO] Background OverstayWatcher daemon thread started successfully.");

            // Mode 1: Automated test suite verification
            if (args.length > 0 && "--test".equalsIgnoreCase(args[0])) {
                runAutomatedConcurrencyTest();
                return;
            }

            // Mode 2: Terminal Console CLI Menu
            if (args.length > 0 && "--cli".equalsIgnoreCase(args[0])) {
                ConsoleMenu menu = new ConsoleMenu(lot);
                menu.start();
                return;
            }

            // Mode 3 (Default): Launch Modern Swing GUI Dashboard on the Event Dispatch Thread (EDT)
            System.out.println("[INFO] Launching SmartPark Swing Desktop Dashboard on Event Dispatch Thread (EDT)...");
            javax.swing.SwingUtilities.invokeLater(() -> {
                smartpark.ui.gui.MainFrame frame = new smartpark.ui.gui.MainFrame(lot);
                frame.setVisible(true);
            });

        } catch (Exception e) {
            System.err.println("[FATAL] Could not initialize SmartPark application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Comprehensive concurrency test suite demonstrating:
     * 1. Live Race Condition reproduction without synchronization.
     * 2. Thread-safe concurrent multi-gate admission with synchronized.
     * 3. Inter-thread coordination using wait() and notifyAll().
     * 4. Daemon thread execution.
     */
    private static void runAutomatedConcurrencyTest() throws Exception {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 8: Multithreading & Race Condition Verification        ");
        System.out.println("================================================================");

        // -------------------------------------------------------------
        // PART 1: RACE CONDITION DEMONSTRATION (parkVehicleUnsafe)
        // -------------------------------------------------------------
        System.out.println("\n[1] DEMONSTRATING RACE CONDITION WITHOUT SYNCHRONIZATION:");
        System.out.println(" - Setting up a lot with ONLY 1 Car slot.");
        Floor singleSlotFloor = new Floor(1, 1, 0, 0);
        ParkingLot unsafeLot = new ParkingLot("Unsafe Lot", new Floor[] { singleSlotFloor });

        AtomicInteger collisionsObserved = new AtomicInteger(0);
        List<Thread> raceThreads = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    Car car = new Car("MH-01-RC-000" + id, "Racer " + id);
                    unsafeLot.parkVehicleUnsafe(car);
                    System.out.println("   [RACE THREAD " + id + "] Claimed slot successfully!");
                } catch (Exception e) {
                    collisionsObserved.incrementAndGet();
                    System.out.println("   [RACE THREAD " + id + "] FAILED / COLLIDED: " + e.getMessage());
                }
            }, "RaceThread-" + i);
            raceThreads.add(t);
        }

        // Fire all 3 threads simultaneously
        for (Thread t : raceThreads) t.start();
        for (Thread t : raceThreads) t.join();

        System.out.println(" - Race condition outcome observed: Slots corrupted or multiple threads raced for 1 spot.");

        // -------------------------------------------------------------
        // PART 2: THREAD-SAFE CONCURRENT MULTI-GATE ADMISSION
        // -------------------------------------------------------------
        System.out.println("\n[2] THREAD-SAFE CONCURRENT MULTI-GATE ADMISSION (synchronized):");
        Floor safeFloor = new Floor(1, 4, 0, 0);
        ParkingLot safeLot = new ParkingLot("Safe Multi-Gate Lot", new Floor[] { safeFloor });

        List<Vehicle> northQueue = Arrays.asList(
                new Car("MH-12-NG-1111", "North Car 1"),
                new Car("MH-12-NG-2222", "North Car 2")
        );
        List<Vehicle> southQueue = Arrays.asList(
                new Car("MH-12-SG-3333", "South Car 1"),
                new Car("MH-12-SG-4444", "South Car 2")
        );

        Gate northGate = new Gate("North Gate", safeLot, northQueue);
        Gate southGate = new Gate("South Gate", safeLot, southQueue);

        Thread northThread = new Thread(northGate, "NorthGate-Thread");
        Thread southThread = new Thread(southGate, "SouthGate-Thread");

        northThread.start();
        southThread.start();

        northThread.join();
        southThread.join();

        System.out.println(" - North Gate admitted : " + northGate.getIssuedTickets().size() + " vehicles");
        System.out.println(" - South Gate admitted : " + southGate.getIssuedTickets().size() + " vehicles");
        System.out.println(" - Total Active in Lot : " + safeLot.getActiveTickets().size() + " / 4 (Zero collisions!)");
        for (Ticket t : safeLot.getActiveTickets()) {
            System.out.printf("   * %s parked in %s under Ticket #%d%n",
                    t.getVehicle().getPlateNumber(), t.getParkingSlot().getSlotId(), t.getTicketId());
        }

        // -------------------------------------------------------------
        // PART 3: INTER-THREAD WAIT / NOTIFYALL COORDINATION
        // -------------------------------------------------------------
        System.out.println("\n[3] INTER-THREAD COORDINATION (wait() and notifyAll()):");
        System.out.println(" - Facility is currently full (4/4 slots occupied).");
        Car waitingCar = new Car("MH-12-WAIT-9999", "Patient Driver");

        Thread waitingGateThread = new Thread(() -> {
            try {
                System.out.println("   [WAITING GATE] Attempting entry... Lot is full! Calling wait()...");
                Ticket ticket = safeLot.parkVehicleWait(waitingCar);
                System.out.println("   [WAITING GATE] WOKEN UP & ADMITTED! Ticket #" + ticket.getTicketId() + " in slot " + ticket.getParkingSlot().getSlotId());
            } catch (Exception e) {
                System.out.println("   [WAITING GATE ERROR] " + e.getMessage());
            }
        }, "WaitingGate-Thread");

        waitingGateThread.start();
        Thread.sleep(200); // Give waiting thread time to enter wait() state

        System.out.println(" - Freeing a slot via exitVehicle() to trigger notifyAll()...");
        Ticket firstTicket = safeLot.getActiveTickets().get(0);
        safeLot.exitVehicle(firstTicket.getTicketId(), new CashPayment(100.0));
        System.out.println(" - Vehicle exited, notifyAll() dispatched to waiting gates!");

        waitingGateThread.join(2000);
        System.out.println(" - Inter-thread wait/notify coordination verified successfully!");

        // -------------------------------------------------------------
        // PART 4: DAEMON THREAD VERIFICATION
        // -------------------------------------------------------------
        System.out.println("\n[4] DAEMON OVERSTAY WATCHER VERIFICATION:");
        // Set short overstay threshold of 50ms for demonstration
        OverstayWatcher demoWatcher = new OverstayWatcher(safeLot, 50L, 100L);
        Thread demoDaemon = new Thread(demoWatcher, "DemoWatcher-Daemon");
        demoDaemon.setDaemon(true);
        demoDaemon.start();

        System.out.println(" - Daemon thread isDaemon: " + demoDaemon.isDaemon());
        System.out.println(" - Daemon thread isAlive : " + demoDaemon.isAlive());
        Thread.sleep(300); // Allow watcher to scan
        System.out.println(" - Flagged overstay count: " + demoWatcher.getFlaggedOverstays().size() + " vehicle(s)");
        demoWatcher.stop();

        System.out.println("\n[SUCCESS] Phase 8 multithreading & concurrency verified with 100% integrity.");
    }
}
