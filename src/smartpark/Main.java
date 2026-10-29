package smartpark;

import smartpark.exception.PaymentFailedException;
import smartpark.model.Car;
import smartpark.model.Truck;
import smartpark.service.CashPayment;
import smartpark.service.Payment;
import smartpark.service.UPIPayment;

/**
 * SmartPark - Entry Point
 * Phase 4: Verification of Payment Strategy Subsystem (Interface, CashPayment, UPIPayment)
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("        SmartPark: Object-Oriented Parking System               ");
        System.out.println("   Phase 4: Payment Strategy Subsystem Verification             ");
        System.out.println("================================================================");

        try {
            Car car = new Car("MH-12-DE-1111", "Rohan Tanwani");
            Truck truck = new Truck("MH-04-TR-5555", "Baljit Logistics", "Diesel", 16.5);

            double carFee = car.calculateFee(3);    // 3 hrs * Rs. 40 = Rs. 120.00
            double truckFee = truck.calculateFee(3); // 3 hrs * Rs. 80 * 1.10 = Rs. 264.00

            System.out.println("\n[1] Fee Summary for Exit Processing:");
            System.out.printf(" - %s Fee: Rs. %.2f%n", car, carFee);
            System.out.printf(" - %s Fee: Rs. %.2f%n", truck, truckFee);

            // 1. Cash Payment Strategy - Exact & Change Calculation
            System.out.println("\n[2] Successful Cash Payment Verification:");
            CashPayment cashSuccess = new CashPayment(200.0);
            System.out.println(" - Before Processing: " + cashSuccess);
            cashSuccess.pay(carFee);
            System.out.println(" - After Processing:  " + cashSuccess);
            System.out.printf(" - Change to return to driver: Rs. %.2f (Expected: Rs. 80.00)%n",
                    cashSuccess.getChangeGiven());

            // 2. Cash Payment Strategy - Insufficient Tender Guard
            System.out.println("\n[3] Insufficient Cash Guard Verification:");
            try {
                System.out.print(" - Attempting to settle Rs. 120.00 fee with Rs. 100.00 cash... ");
                CashPayment cashShort = new CashPayment(100.0);
                cashShort.pay(carFee);
            } catch (PaymentFailedException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            // 3. UPI Payment Strategy - Successful Digital Settlement
            System.out.println("\n[4] Successful UPI Payment Verification:");
            UPIPayment upiSuccess = new UPIPayment("rohan@okhdfcbank");
            System.out.println(" - Before Processing: " + upiSuccess);
            upiSuccess.pay(truckFee);
            System.out.println(" - After Processing:  " + upiSuccess);
            System.out.println(" - Generated Txn Reference: " + upiSuccess.getTransactionRef());

            // 4. UPI Payment Strategy - Invalid VPA Guard
            System.out.println("\n[5] Invalid UPI ID Guard Verification:");
            try {
                System.out.print(" - Attempting to use UPI ID without '@' sign ('invalidVpaHandle')... ");
                new UPIPayment("invalidVpaHandle");
            } catch (PaymentFailedException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            try {
                System.out.print(" - Attempting to use UPI ID with missing provider ('rohan@')... ");
                new UPIPayment("rohan@");
            } catch (PaymentFailedException e) {
                System.out.println("CAUGHT EXPECTED EXCEPTION: " + e.getMessage());
            }

            // 5. Polymorphic Strategy Dispatch (Interface Substitution)
            System.out.println("\n[6] Dynamic Strategy Dispatch (Interface Realization):");
            Payment[] paymentMethods = new Payment[] {
                    new CashPayment(150.0),
                    new UPIPayment("priya@icici")
            };

            double feeToSettle = 100.0;
            for (Payment method : paymentMethods) {
                // The caller interacts ONLY with the Payment interface contract
                System.out.printf(" - Executing payment via %-4s channel: ", method.getMode());
                method.pay(feeToSettle);
                System.out.println("SUCCESS -> " + method);
            }

            System.out.println("\n[SUCCESS] Phase 4 complete: Payment strategy subsystem verified successfully.");

        } catch (Exception e) {
            System.err.println("[ERROR] Unexpected exception occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
