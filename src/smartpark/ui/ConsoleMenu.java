package smartpark.ui;

import smartpark.exception.DuplicateVehicleException;
import smartpark.exception.InvalidVehicleException;
import smartpark.exception.LotFullException;
import smartpark.exception.PaymentFailedException;
import smartpark.exception.TicketNotFoundException;
import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.model.Vehicle;
import smartpark.service.CashPayment;
import smartpark.service.ParkingLot;
import smartpark.service.Payment;
import smartpark.service.UPIPayment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive Terminal Console Menu for SmartPark.
 * Demonstrates:
 * 1. Resilient scanner input parsing (eliminates nextInt/nextLine trailing newline bugs).
 * 2. Comprehensive try-catch shielding so invalid input or domain failures never crash the menu loop.
 * 3. Decoupling: UI layer is isolated from business rules in ParkingLot.
 */
public class ConsoleMenu {
    private final ParkingLot parkingLot;
    private final Scanner scanner;
    private boolean running;

    public ConsoleMenu(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
        this.scanner = new Scanner(System.in);
        this.running = false;
    }

    /**
     * Starts the interactive command-line interface loop.
     */
    public void start() {
        this.running = true;
        printBanner();

        while (running) {
            printMenu();
            int choice = readInt("Select an option (1-6): ");

            try {
                switch (choice) {
                    case 1:
                        handleParkVehicle();
                        break;
                    case 2:
                        handleExitVehicle();
                        break;
                    case 3:
                        handleViewStatus();
                        break;
                    case 4:
                        handleViewActiveTickets();
                        break;
                    case 5:
                        handleViewTicketHistory();
                        break;
                    case 6:
                        handleExitSystem();
                        break;
                    default:
                        System.out.println("[WARNING] Invalid option. Please enter a number between 1 and 6.\n");
                }
            } catch (Exception e) {
                System.out.println("\n[ERROR] An unexpected error occurred: " + e.getMessage());
            }
        }
    }

    private void printBanner() {
        System.out.println("================================================================");
        System.out.println("          🚗 Welcome to SmartPark Management System 🚗          ");
        System.out.println("             Facility: " + parkingLot.getLotName());
        System.out.println("================================================================");
    }

    private void printMenu() {
        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|                     OPERATIONS DASHBOARD                     |");
        System.out.println("+--------------------------------------------------------------+");
        System.out.println("|  [1] Park Vehicle (Car, Bike, Truck)                         |");
        System.out.println("|  [2] Exit Vehicle & Collect Payment (Cash / UPI)             |");
        System.out.println("|  [3] View Facility Status & Capacity Breakdown               |");
        System.out.println("|  [4] View Currently Active Tickets                           |");
        System.out.println("|  [5] View Settled Ticket History                             |");
        System.out.println("|  [6] Exit System                                             |");
        System.out.println("+--------------------------------------------------------------+");
    }

    private void handleParkVehicle() {
        System.out.println("\n--- [1] Park a Vehicle ---");
        System.out.println("Vehicle Category: [1] Car  |  [2] Bike  |  [3] Truck");
        int typeChoice = readInt("Select vehicle type (1-3): ");

        if (typeChoice < 1 || typeChoice > 3) {
            System.out.println("[ERROR] Invalid vehicle type selected. Aborting.");
            return;
        }

        String plate = readString("Enter license plate number (e.g. MH-12-AB-1234): ");
        String owner = readString("Enter owner full name: ");

        try {
            Vehicle vehicle;
            if (typeChoice == 1) {
                String fuel = readStringWithDefault("Enter fuel type [Petrol/Diesel/Electric/CNG] (default: Petrol): ", "Petrol");
                vehicle = new Car(plate, owner, fuel);
            } else if (typeChoice == 2) {
                String fuel = readStringWithDefault("Enter fuel type [Petrol/Electric] (default: Petrol): ", "Petrol");
                vehicle = new Bike(plate, owner, fuel);
            } else {
                String fuel = readStringWithDefault("Enter fuel type [Diesel/Electric/CNG] (default: Diesel): ", "Diesel");
                double capacity = readDouble("Enter truck load capacity in metric tonnes (e.g. 8.5): ");
                vehicle = new Truck(plate, owner, fuel, capacity);
            }

            Ticket ticket = parkingLot.parkVehicle(vehicle);

            System.out.println("\n>>> ENTRY SUCCESSFUL! Ticket Issued <<<");
            System.out.println("----------------------------------------------------------------");
            System.out.printf("Ticket ID       : #%d%n", ticket.getTicketId());
            System.out.printf("Vehicle         : %s%n", vehicle);
            System.out.printf("Assigned Slot   : %s (Floor %s)%n",
                    ticket.getParkingSlot().getSlotId(),
                    ticket.getParkingSlot().getSlotId().substring(1, 2));
            System.out.printf("Hourly Rate     : Rs. %.2f / hr%n", vehicle.calculateFee(1));
            System.out.printf("Admission Time  : %s%n", formatTime(ticket.getEntryTimeMillis()));
            System.out.println("----------------------------------------------------------------");

        } catch (DuplicateVehicleException e) {
            System.out.println("[REJECTED] Duplicate entry: " + e.getMessage());
        } catch (LotFullException e) {
            System.out.println("[REJECTED] Capacity exhausted: " + e.getMessage());
        } catch (InvalidVehicleException e) {
            System.out.println("[REJECTED] Input validation failed: " + e.getMessage());
        }
    }

    private void handleExitVehicle() {
        System.out.println("\n--- [2] Vehicle Departure & Settle Payment ---");
        int ticketId = readInt("Enter Ticket ID to checkout: ");

        Ticket activeTicket = parkingLot.findActiveTicketById(ticketId);
        if (activeTicket == null) {
            System.out.println("[ERROR] Active ticket #" + ticketId + " was not found in the lot.");
            return;
        }

        long now = System.currentTimeMillis();
        int billedHours = activeTicket.calculateHoursParked(now);
        double fee = activeTicket.getVehicle().calculateFee(billedHours);

        System.out.println("\n>>> TICKET SUMMARY <<<");
        System.out.println("----------------------------------------------------------------");
        System.out.printf("Ticket ID       : #%d%n", activeTicket.getTicketId());
        System.out.printf("Vehicle         : %s%n", activeTicket.getVehicle());
        System.out.printf("Parked In Slot  : %s%n", activeTicket.getParkingSlot().getSlotId());
        System.out.printf("Admission Time  : %s%n", formatTime(activeTicket.getEntryTimeMillis()));
        System.out.printf("Departure Time  : %s%n", formatTime(now));
        System.out.printf("Billed Duration : %d hour(s)%n", billedHours);
        System.out.printf("Total Fee Due   : Rs. %.2f%n", fee);
        System.out.println("----------------------------------------------------------------");

        System.out.println("Payment Mode: [1] Cash  |  [2] UPI");
        int payChoice = readInt("Select payment mode (1-2): ");

        Payment paymentMethod;
        try {
            if (payChoice == 1) {
                double tendered = readDouble(String.format("Enter cash tendered by driver (Minimum Rs. %.2f): ", fee));
                paymentMethod = new CashPayment(tendered);
            } else if (payChoice == 2) {
                String upi = readString("Enter customer UPI ID (e.g. name@bank): ");
                paymentMethod = new UPIPayment(upi);
            } else {
                System.out.println("[ERROR] Invalid payment mode selected. Departure aborted.");
                return;
            }

            // Transactional exit call: payment collected first before vacating slot
            Ticket closedTicket = parkingLot.exitVehicle(ticketId, paymentMethod, now);

            System.out.println("\n>>> PAYMENT & DEPARTURE COMPLETED! <<<");
            System.out.println("----------------------------------------------------------------");
            System.out.printf("Status          : SETTLED & CLOSED%n");
            System.out.printf("Payment Channel : %s%n", paymentMethod.getMode());
            if (paymentMethod instanceof CashPayment) {
                CashPayment cp = (CashPayment) paymentMethod;
                System.out.printf("Cash Tendered   : Rs. %.2f%n", cp.getCashTendered());
                System.out.printf("Change Returned : Rs. %.2f%n", cp.getChangeGiven());
            } else if (paymentMethod instanceof UPIPayment) {
                UPIPayment up = (UPIPayment) paymentMethod;
                System.out.printf("UPI VPA         : %s%n", up.getUpiId());
                System.out.printf("Transaction Ref : %s%n", up.getTransactionRef());
            }
            System.out.printf("Slot Freed      : %s is now available.%n", closedTicket.getParkingSlot().getSlotId());
            System.out.println("----------------------------------------------------------------");

        } catch (PaymentFailedException e) {
            System.out.println("\n[TRANSACTION FAILED] " + e.getMessage());
            System.out.println("[SAFETY GUARANTEE] Slot remains OCCUPIED and Ticket remains ACTIVE until payment settles.");
        } catch (TicketNotFoundException | InvalidVehicleException e) {
            System.out.println("\n[ERROR] Departure failed: " + e.getMessage());
        }
    }

    private void handleViewStatus() {
        System.out.println("\n" + parkingLot.getStatus());
    }

    private void handleViewActiveTickets() {
        List<Ticket> active = parkingLot.getActiveTickets();
        System.out.println("\n--- Active Tickets Currently Parked (" + active.size() + ") ---");
        if (active.isEmpty()) {
            System.out.println("No vehicles are currently parked in the facility.");
            return;
        }

        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-16s | %-8s | %-12s | %-10s | %-19s%n",
                "Ticket", "Plate Number", "Type", "Slot", "Owner", "Entry Time");
        System.out.println("--------------------------------------------------------------------------------------------");
        for (Ticket t : active) {
            System.out.printf("#%-7d | %-16s | %-8s | %-12s | %-10s | %-19s%n",
                    t.getTicketId(),
                    t.getVehicle().getPlateNumber(),
                    t.getVehicle().getVehicleType(),
                    t.getParkingSlot().getSlotId(),
                    t.getVehicle().getOwnerName(),
                    formatTime(t.getEntryTimeMillis()));
        }
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    private void handleViewTicketHistory() {
        List<Ticket> history = parkingLot.getTicketHistory();
        System.out.println("\n--- Completed Ticket History (" + history.size() + ") ---");
        if (history.isEmpty()) {
            System.out.println("No completed parking sessions recorded yet.");
            return;
        }

        System.out.println("--------------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-16s | %-8s | %-10s | %-19s | %-19s%n",
                "Ticket", "Plate Number", "Type", "Slot", "Entry Time", "Exit Time");
        System.out.println("--------------------------------------------------------------------------------------------");
        for (Ticket t : history) {
            System.out.printf("#%-7d | %-16s | %-8s | %-10s | %-19s | %-19s%n",
                    t.getTicketId(),
                    t.getVehicle().getPlateNumber(),
                    t.getVehicle().getVehicleType(),
                    t.getParkingSlot().getSlotId(),
                    formatTime(t.getEntryTimeMillis()),
                    formatTime(t.getExitTimeMillis()));
        }
        System.out.println("--------------------------------------------------------------------------------------------");
    }

    private void handleExitSystem() {
        System.out.println("\nThank you for using SmartPark. Shutting down system gracefully...");
        this.running = false;
    }

    // =========================================================================
    // Resilient Input Reading Utilities (Guards Against Scanner Buffer Pitfalls)
    // =========================================================================

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("[INPUT ERROR] Please enter a valid integer number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double val = Double.parseDouble(input);
                if (val <= 0.0) {
                    System.out.println("[INPUT ERROR] Amount must be greater than zero.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("[INPUT ERROR] Please enter a valid numeric decimal amount.");
            }
        }
    }

    private String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("[INPUT ERROR] Input cannot be blank.");
        }
    }

    private String readStringWithDefault(String prompt, String defaultValue) {
        System.out.print(prompt);
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }

    private String formatTime(long millis) {
        if (millis <= 0L) {
            return "N/A";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
    }
}
