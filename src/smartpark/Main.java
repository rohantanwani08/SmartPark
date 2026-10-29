package smartpark;

import smartpark.exception.DuplicateVehicleException;
import smartpark.exception.LotFullException;
import smartpark.exception.PaymentFailedException;
import smartpark.exception.TicketNotFoundException;
import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.service.CashPayment;
import smartpark.service.ParkingLot;
import smartpark.service.UPIPayment;

/**
 * SmartPark - Entry Point
 * Phase 5: Verification of Core ParkingLot Service (Multi-floor allocation, duplicate detection, transactional exit)
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 5: Core ParkingLot Service Verification                ");
        System.out.println("================================================================");

        try {
            // 1. Initialize Multi-Floor Facility
            // Floor 1: 2 Car slots, 1 Bike slot, 1 Truck slot (Total: 4 slots)
            // Floor 2: 2 Car slots, 1 Bike slot, 0 Truck slots (Total: 3 slots)
            Floor floor1 = new Floor(1, 2, 1, 1);
            Floor floor2 = new Floor(2, 2, 1, 0);
            ParkingLot lot = new ParkingLot("Phoenix Grand Mall Plaza", new Floor[] { floor1, floor2 });

            System.out.println("\n[1] Facility Initialized:");
            System.out.println(lot.getStatus());

            // 2. Multi-Floor Overflow Parking Test
            System.out.println("\n[2] Multi-Floor Allocation & Overflow Test:");
            Car car1 = new Car("MH-12-AA-1001", "Rohan Tanwani");
            Car car2 = new Car("MH-12-BB-2002", "Priya Sharma");
            Car car3 = new Car("MH-12-CC-3003", "Aniket Joshi"); // Should overflow to Floor 2!

            Ticket t1 = lot.parkVehicle(car1);
            Ticket t2 = lot.parkVehicle(car2);
            Ticket t3 = lot.parkVehicle(car3);

            System.out.println(" - Car 1 assigned to: " + t1.getParkingSlot().getSlotId() + " (Expected: Floor 1)");
            System.out.println(" - Car 2 assigned to: " + t2.getParkingSlot().getSlotId() + " (Expected: Floor 1)");
            System.out.println(" - Car 3 assigned to: " + t3.getParkingSlot().getSlotId() + " (Expected: Floor 2 Overflow)");

            // 3. Duplicate License Plate Guard Test
            System.out.println("\n[3] Duplicate Vehicle Detection Guard:");
            try {
                System.out.print(" - Attempting to park car with already-active plate 'MH-12-AA-1001'... ");
                Car duplicateCar = new Car("MH-12-AA-1001", "Imposter Driver");
                lot.parkVehicle(duplicateCar);
            } catch (DuplicateVehicleException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            // 4. Lot Full Guard Test
            System.out.println("\n[4] Lot Full Guard Test:");
            Truck truck1 = new Truck("MH-04-TR-4001", "National Logistics", "Diesel", 12.0);
            Ticket tTruck = lot.parkVehicle(truck1);
            System.out.println(" - Truck 1 parked in: " + tTruck.getParkingSlot().getSlotId());

            try {
                System.out.print(" - Attempting to park 2nd Truck when facility has only 1 truck slot... ");
                Truck truck2 = new Truck("MH-04-TR-4002", "Speed Cargo", "Diesel", 8.0);
                lot.parkVehicle(truck2);
            } catch (LotFullException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            // 5. Transactional Exit Guarantee: Payment Failure Test
            System.out.println("\n[5] Transactional Exit Verification (Failed Payment Integrity):");
            // Simulate 2 hours parked for Car 1 (Fee = 2 hrs * Rs. 40 = Rs. 80.00)
            long simulatedExitTime = t1.getEntryTimeMillis() + (2 * 3600 * 1000L);
            CashPayment insufficientCash = new CashPayment(50.0); // Driver only has Rs. 50

            try {
                System.out.print(" - Attempting exit with insufficient cash (Tendered Rs. 50 for Rs. 80 fee)... ");
                lot.exitVehicle(t1.getTicketId(), insufficientCash, simulatedExitTime);
            } catch (PaymentFailedException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            // Verify state is completely intact!
            System.out.println(" - Checking Ticket #1 status after failed payment: Active? " + t1.isActive());
            System.out.println(" - Checking Slot " + t1.getParkingSlot().getSlotId() + " status: Available? " + t1.getParkingSlot().isAvailable());
            System.out.println(" - Current Lot Revenue: Rs. " + lot.getTotalRevenue() + " (Expected: Rs. 0.00)");

            // 6. Transactional Exit Guarantee: Successful Exit
            System.out.println("\n[6] Successful Exit Verification (State Mutation):");
            // Retry Car 1 exit with sufficient cash
            CashPayment sufficientCash = new CashPayment(100.0);
            Ticket settledTicket = lot.exitVehicle(t1.getTicketId(), sufficientCash, simulatedExitTime);
            System.out.println(" - Exit Success! Ticket closed: " + settledTicket);
            System.out.printf(" - Cash Returned to Driver: Rs. %.2f%n", sufficientCash.getChangeGiven());
            System.out.println(" - Slot " + settledTicket.getParkingSlot().getSlotId() + " is now free? " + settledTicket.getParkingSlot().isAvailable());
            System.out.printf(" - Updated Total Revenue: Rs. %.2f%n", lot.getTotalRevenue());

            // Exit Truck with UPI (3 hours parked, 12T heavy truck: 3 * 80 * 1.10 = Rs. 264.00)
            long truckExitTime = tTruck.getEntryTimeMillis() + (3 * 3600 * 1000L);
            UPIPayment upi = new UPIPayment("logistics@hdfcbank");
            lot.exitVehicle(tTruck.getTicketId(), upi, truckExitTime);
            System.out.println(" - Truck Exit Success via UPI! TxnRef: " + upi.getTransactionRef());
            System.out.printf(" - Updated Total Revenue: Rs. %.2f%n", lot.getTotalRevenue());

            // 7. Non-existent Ticket Lookup Guard
            System.out.println("\n[7] Non-existent Ticket Guard:");
            try {
                System.out.print(" - Attempting exit with invalid ticket ID #9999... ");
                lot.exitVehicle(9999, new CashPayment(100.0));
            } catch (TicketNotFoundException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            // 8. Final Status Report
            System.out.println("\n[8] Final Facility Status Report:");
            System.out.println(lot.getStatus());

            System.out.println("\n[SUCCESS] Phase 5 complete: Core ParkingLot service verified successfully.");

        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
