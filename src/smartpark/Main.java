package smartpark;

import smartpark.exception.InvalidVehicleException;
import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.ParkingSlot;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.model.Vehicle;

/**
 * SmartPark - Entry Point
 * Phase 3: Verification of ParkingSlot, Floor (Composition), and Ticket (Static Counter & Duration Math)
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 3: Slot, Floor & Ticket Verification                   ");
        System.out.println("================================================================");

        try {
            // 1. Composition Verification: Floor manages ParkingSlot[]
            // Floor 1 with 2 Car slots, 2 Bike slots, 1 Truck slot
            Floor floor1 = new Floor(1, 2, 2, 1);
            System.out.println("\n[1] Floor Initialization (Composition):");
            System.out.println(" - " + floor1);
            System.out.println(" - Total Slots Allocated: " + floor1.getTotalSlots());
            System.out.println(" - Free Car Slots: " + floor1.getFreeCount("Car"));
            System.out.println(" - Free Bike Slots: " + floor1.getFreeCount("Bike"));
            System.out.println(" - Free Truck Slots: " + floor1.getFreeCount("Truck"));

            // 2. Vehicle Creation
            Car car1 = new Car("MH-12-AB-1001", "Rohan Tanwani");
            Car car2 = new Car("MH-12-CD-2002", "Priya Sharma");
            Bike bike1 = new Bike("MH-14-XY-3003", "Amit Verma");
            Truck truck1 = new Truck("MH-04-TR-4004", "Baljit Logistics", "Diesel", 14.0);

            // 3. Slot Finding & Ticket Generation (Static Counter Verification)
            System.out.println("\n[2] Parking Vehicles & Generating Tickets (Static Counter):");

            ParkingSlot carSlot1 = floor1.findFreeSlot(car1);
            carSlot1.assignVehicle(car1);
            Ticket t1 = new Ticket(car1, carSlot1);
            System.out.println(" - Issued: " + t1);

            ParkingSlot carSlot2 = floor1.findFreeSlot(car2);
            carSlot2.assignVehicle(car2);
            Ticket t2 = new Ticket(car2, carSlot2);
            System.out.println(" - Issued: " + t2);

            ParkingSlot bikeSlot1 = floor1.findFreeSlot(bike1);
            bikeSlot1.assignVehicle(bike1);
            Ticket t3 = new Ticket(bike1, bikeSlot1);
            System.out.println(" - Issued: " + t3);

            ParkingSlot truckSlot1 = floor1.findFreeSlot(truck1);
            truckSlot1.assignVehicle(truck1);
            Ticket t4 = new Ticket(truck1, truckSlot1);
            System.out.println(" - Issued: " + t4);

            System.out.println("\n[3] Post-Parking Floor Status:");
            System.out.println(" - " + floor1);
            System.out.println(" - Free Car Slots remaining: " + floor1.getFreeCount("Car"));
            System.out.println(" - Free Bike Slots remaining: " + floor1.getFreeCount("Bike"));
            System.out.println(" - Free Truck Slots remaining: " + floor1.getFreeCount("Truck"));

            // 4. Duration Calculation Math Verification
            System.out.println("\n[4] Ticket Duration Math Verification:");
            long entry = t1.getEntryTimeMillis();

            // Scenario A: Parked for 15 minutes (900,000 ms) -> rounds up to 1 hour (minimum rule)
            long exit15Min = entry + (15 * 60 * 1000L);
            int hours15Min = t1.calculateHoursParked(exit15Min);
            System.out.println(" - 15 minutes parked -> Billed: " + hours15Min + " hr (Expected: 1 hr)");

            // Scenario B: Parked for 65 minutes (3,900,000 ms) -> rounds up to 2 hours (ceiling rule)
            long exit65Min = entry + (65 * 60 * 1000L);
            int hours65Min = t1.calculateHoursParked(exit65Min);
            System.out.println(" - 65 minutes parked -> Billed: " + hours65Min + " hrs (Expected: 2 hrs)");

            // Scenario C: Parked for 180 minutes -> exactly 3 hours
            long exit3Hrs = entry + (3 * 3600 * 1000L);
            int hours3Hrs = t1.calculateHoursParked(exit3Hrs);
            System.out.println(" - 180 minutes parked -> Billed: " + hours3Hrs + " hrs (Expected: 3 hrs)");

            // 5. Incompatible Slot Parking Check
            System.out.println("\n[5] Slot Guard Check (Incompatible Vehicle Type):");
            try {
                System.out.print(" - Attempting to park Truck into Bike slot... ");
                bikeSlot1.vacate(); // temporarily vacate bike slot
                bikeSlot1.assignVehicle(truck1); // Truck should NOT fit in Bike slot!
            } catch (InvalidVehicleException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
                bikeSlot1.assignVehicle(bike1); // restore
            }

            // 6. Slot Vacating Verification
            System.out.println("\n[6] Slot Vacating Verification:");
            System.out.println(" - Slot before vacating: " + carSlot1);
            Vehicle departed = carSlot1.vacate();
            System.out.println(" - Vehicle departed: " + departed.getPlateNumber());
            System.out.println(" - Slot after vacating: " + carSlot1);
            System.out.println(" - Is slot available now? " + carSlot1.isAvailable());

            System.out.println("\n[SUCCESS] Phase 3 complete: Slot, Floor, and Ticket verified successfully.");

        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
