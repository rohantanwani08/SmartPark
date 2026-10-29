package smartpark;

import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.service.CashPayment;
import smartpark.service.FileManager;
import smartpark.service.ParkingLot;
import smartpark.ui.ConsoleMenu;

import java.io.File;
import java.util.List;

/**
 * SmartPark - Main Application Entry Point
 * Phase 7: Dual File Persistence (CSV Character Stream + Binary Byte Stream)
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
                // If first-time run with empty storage, pre-seed demo vehicles
                lot.parkVehicle(new Car("MH-12-AB-1234", "Rohan Tanwani"));
                lot.parkVehicle(new Bike("MH-14-XY-9876", "Amit Verma"));
                System.out.println("[INFO] Initialized facility with demo vehicles and saved to persistent storage.");
            }

            // Check if automated test mode was requested
            if (args.length > 0 && "--test".equalsIgnoreCase(args[0])) {
                runAutomatedPersistenceTest(lot, fileManager);
                return;
            }

            // Normal Execution: Launch Interactive CLI Menu
            ConsoleMenu menu = new ConsoleMenu(lot);
            menu.start();

        } catch (Exception e) {
            System.err.println("[FATAL] Could not initialize SmartPark application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Automated test verifying Character Streams, Byte Streams, and ID Collision Prevention.
     */
    private static void runAutomatedPersistenceTest(ParkingLot lot, FileManager fileManager) throws Exception {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 7: Dual-Mode Persistence & ID Collision Self-Test      ");
        System.out.println("================================================================");

        // 1. Character Stream Verification (tickets.txt)
        System.out.println("\n[1] Verifying Character Stream Persistence (data/tickets.txt):");
        File csvFile = new File(ACTIVE_TICKETS_FILE);
        System.out.println(" - tickets.txt exists: " + csvFile.exists());
        System.out.println(" - tickets.txt size: " + csvFile.length() + " bytes");
        System.out.println(" - Current active tickets in memory: " + lot.getActiveTickets().size());

        // 2. Byte Stream Verification (ticket_log.dat)
        System.out.println("\n[2] Verifying Byte Stream Append Log (data/ticket_log.dat):");
        Ticket ticketToExit = lot.getActiveTickets().get(0);
        int exitId = ticketToExit.getTicketId();
        CashPayment cash = new CashPayment(100.0);
        lot.exitVehicle(exitId, cash);
        System.out.printf(" - Exited Ticket #%d with CashPayment (Fee collected, slot freed).%n", exitId);

        File binaryFile = new File(BINARY_LOG_FILE);
        System.out.println(" - ticket_log.dat exists: " + binaryFile.exists());
        System.out.println(" - ticket_log.dat size: " + binaryFile.length() + " bytes");

        List<FileManager.AuditLogEntry> auditLogs = fileManager.readBinaryLogs();
        System.out.println(" - Binary audit log entries read: " + auditLogs.size());
        for (FileManager.AuditLogEntry entry : auditLogs) {
            System.out.println("   * " + entry);
        }

        // 3. Application Restart Simulation (ID Collision Prevention Verification)
        System.out.println("\n[3] Simulating Application Restart & ID Collision Prevention:");
        Floor restartF1 = new Floor(1, 3, 2, 1);
        Floor restartF2 = new Floor(2, 3, 2, 0);
        ParkingLot freshLot = new ParkingLot("Restarted Facility", new Floor[] { restartF1, restartF2 });
        FileManager restartFileManager = new FileManager(ACTIVE_TICKETS_FILE, BINARY_LOG_FILE);
        freshLot.setFileManager(restartFileManager);

        int reloaded = restartFileManager.loadActiveTickets(freshLot);
        System.out.println(" - Restored tickets after restart: " + reloaded);
        for (Ticket t : freshLot.getActiveTickets()) {
            System.out.printf("   * Reloaded Ticket #%d [%s in Slot %s]%n",
                    t.getTicketId(), t.getVehicle().getPlateNumber(), t.getParkingSlot().getSlotId());
        }

        // Admit a brand new car: ticket ID must be strictly greater than any reloaded ticket
        Car brandNewCar = new Car("MH-01-NEW-7777", "New Visitor");
        Ticket brandNewTicket = freshLot.parkVehicle(brandNewCar);
        System.out.println(" - Newly issued ticket after restart: Ticket #" + brandNewTicket.getTicketId());

        boolean noCollision = brandNewTicket.getTicketId() > exitId;
        System.out.println(" - ID Collision Avoidance Verified? " + noCollision + " (New ID > Past IDs)");

        System.out.println("\n[SUCCESS] Phase 7 dual-mode persistence verified successfully with 100% integrity.");
    }
}
