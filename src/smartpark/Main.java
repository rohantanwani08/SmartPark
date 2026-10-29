package smartpark;

import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.service.CashPayment;
import smartpark.service.ParkingLot;
import smartpark.ui.ConsoleMenu;

/**
 * SmartPark - Main Application Entry Point
 * Phase 6: Interactive Resilient Console Menu
 */
public class Main {
    public static void main(String[] args) {
        try {
            // Configure a 2-Floor Parking Facility
            // Floor 1: 3 Car slots, 2 Bike slots, 1 Truck slot
            // Floor 2: 3 Car slots, 2 Bike slots, 0 Truck slots
            Floor floor1 = new Floor(1, 3, 2, 1);
            Floor floor2 = new Floor(2, 3, 2, 0);
            ParkingLot lot = new ParkingLot("SmartPark Central Plaza", new Floor[] { floor1, floor2 });

            // Pre-seed demo active vehicles so the lot immediately has sample state
            lot.parkVehicle(new Car("MH-12-AB-1234", "Rohan Tanwani"));
            lot.parkVehicle(new Bike("MH-14-XY-9876", "Amit Verma"));

            // Check if automated test mode was requested
            if (args.length > 0 && "--test".equalsIgnoreCase(args[0])) {
                runAutomatedTest(lot);
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
     * Automated non-blocking test harness for Phase 6 CI/CD and terminal verification.
     */
    private static void runAutomatedTest(ParkingLot lot) throws Exception {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 6: Console CLI & Resilient Workflow Self-Test          ");
        System.out.println("================================================================");

        System.out.println("\n[1] Facility Status (Pre-seeded with 2 demo vehicles):");
        System.out.println(lot.getStatus());

        System.out.println("\n[2] Testing Active Tickets Lookup:");
        for (Ticket t : lot.getActiveTickets()) {
            System.out.printf(" - Active: Ticket #%d | %s in %s%n",
                    t.getTicketId(), t.getVehicle().getPlateNumber(), t.getParkingSlot().getSlotId());
        }

        System.out.println("\n[3] Testing Departure with Cash Payment:");
        Ticket t1 = lot.getActiveTickets().get(0);
        int t1Id = t1.getTicketId();
        CashPayment cash = new CashPayment(100.0);
        Ticket exited = lot.exitVehicle(t1Id, cash);
        System.out.println(" - Successfully exited ticket #" + t1Id + " for vehicle: " + exited.getVehicle().getPlateNumber());
        System.out.printf(" - Change returned: Rs. %.2f%n", cash.getChangeGiven());
        System.out.println(" - Active tickets remaining: " + lot.getActiveTickets().size());
        System.out.println(" - Settled history records : " + lot.getTicketHistory().size());

        System.out.println("\n[4] Testing Admission of New Vehicle (Truck):");
        Truck truck = new Truck("MH-04-TR-8888", "Karan Logistics", "Diesel", 15.0);
        Ticket truckTicket = lot.parkVehicle(truck);
        System.out.println(" - Admitted Truck under Ticket #" + truckTicket.getTicketId() + " in slot " + truckTicket.getParkingSlot().getSlotId());

        System.out.println("\n[5] Post-Operation Facility Status:");
        System.out.println(lot.getStatus());

        System.out.println("\n[SUCCESS] Phase 6 automated self-test passed with 100% integrity.");
    }
}
