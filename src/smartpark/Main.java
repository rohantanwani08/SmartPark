package smartpark;

import smartpark.exception.InvalidVehicleException;
import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Truck;
import smartpark.model.Vehicle;

/**
 * SmartPark - Entry Point
 * Phase 2: Verification of Exception Hierarchy and Polymorphic Vehicle Classes
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 2: Vehicle Hierarchy & Polymorphism Verification        ");
        System.out.println("================================================================");

        try {
            // 1. Constructor Chaining Verification
            // Car(plate, owner) uses this(...) to delegate to Car(plate, owner, "Petrol")
            Car carDefault = new Car("MH-12-DE-1111", "Rohan Tanwani");
            Car carEv = new Car("MH-12-EV-2222", "Priya Sharma", "Electric");

            // 2. Specialized Vehicle Types
            Bike bike = new Bike("MH-14-BK-3333", "Amit Verma");
            Truck lightTruck = new Truck("MH-04-TR-4444", "Karan Singh", "Diesel", 8.0);
            Truck heavyTruck = new Truck("MH-04-TR-5555", "Baljit Logistics", "Diesel", 16.5);

            System.out.println("\n[1] Instantiated Vehicles (Encapsulation & State Verified):");
            Vehicle[] fleet = new Vehicle[] { carDefault, carEv, bike, lightTruck, heavyTruck };
            for (Vehicle v : fleet) {
                System.out.println(" - " + v);
            }

            // 3. Dynamic Method Dispatch (Runtime Polymorphism)
            int testHours = 3;
            System.out.println("\n[2] Polymorphic Fee Calculation for " + testHours + " Hours:");
            System.out.println("----------------------------------------------------------------");
            System.out.printf("%-10s | %-16s | %-12s | %-10s%n", "Type", "Plate Number", "Owner", "Fee (INR)");
            System.out.println("----------------------------------------------------------------");

            for (Vehicle v : fleet) {
                // v.calculateFee(hours) is resolved dynamically at runtime by JVM vtable
                double fee = v.calculateFee(testHours);
                System.out.printf("%-10s | %-16s | %-12s | Rs. %-8.2f%n",
                        v.getVehicleType(), v.getPlateNumber(), v.getOwnerName(), fee);
            }
            System.out.println("----------------------------------------------------------------");

            // 4. Heavy Truck Surcharge Verification
            System.out.println("\n[3] Surcharge Verification:");
            System.out.println(" - Light Truck (8T <= 10T) Fee for 1 hr: Rs. " + lightTruck.calculateFee(1) + " (Expected: Rs. 80.00)");
            System.out.println(" - Heavy Truck (16.5T > 10T) Fee for 1 hr: Rs. " + heavyTruck.calculateFee(1) + " (Expected: Rs. 88.00 [80 + 10%])");

            // 5. Custom Checked Exception Verification
            System.out.println("\n[4] Exception Handling Verification:");
            try {
                System.out.print(" - Attempting invalid plate creation... ");
                new Car("", "Invalid Plate User");
            } catch (InvalidVehicleException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            try {
                System.out.print(" - Attempting negative truck capacity creation... ");
                new Truck("MH-01-TR-9999", "Bad Cargo", "Diesel", -5.0);
            } catch (InvalidVehicleException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            System.out.println("\n[SUCCESS] Phase 2 complete: All OOP principles verified successfully.");

        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
