package smartpark.service;

import smartpark.exception.InvalidVehicleException;
import smartpark.model.Bike;
import smartpark.model.Car;
import smartpark.model.Floor;
import smartpark.model.ParkingSlot;
import smartpark.model.Ticket;
import smartpark.model.Truck;
import smartpark.model.Vehicle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles Dual-Mode File Persistence for the SmartPark system.
 * Demonstrates:
 * 1. Character Streams (BufferedReader / BufferedWriter) for human-readable CSV active tickets.
 * 2. Byte Streams (DataOutputStream / DataInputStream on FileOutputStream append mode) for binary audit log.
 * 3. Prevention of the Auto-Increment ID Collision Trap upon state reload.
 */
public class FileManager {
    private final String activeTicketsPath;
    private final String binaryLogPath;

    public FileManager(String activeTicketsPath, String binaryLogPath) {
        this.activeTicketsPath = activeTicketsPath;
        this.binaryLogPath = binaryLogPath;
        ensureDirectoriesExist();
    }

    private void ensureDirectoriesExist() {
        File activeFile = new File(activeTicketsPath);
        if (activeFile.getParentFile() != null) {
            activeFile.getParentFile().mkdirs();
        }
        File logFile = new File(binaryLogPath);
        if (logFile.getParentFile() != null) {
            logFile.getParentFile().mkdirs();
        }
    }

    // =========================================================================
    // 1. CHARACTER STREAMS: CSV Active Tickets (BufferedReader & BufferedWriter)
    // =========================================================================

    /**
     * Persists all currently active tickets to disk as CSV lines using BufferedWriter.
     * Schema: ticketId,plateNumber,ownerName,vehicleType,fuelType,extraParam,slotId,entryTimeMillis
     *
     * @param activeTickets list of currently active tickets
     * @throws IOException if disk write fails
     */
    public void saveActiveTickets(List<Ticket> activeTickets) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(activeTicketsPath, false))) {
            for (Ticket ticket : activeTickets) {
                Vehicle v = ticket.getVehicle();
                String extra = "";
                String fuel = "";

                if (v instanceof smartpark.model.MotorVehicle) {
                    fuel = ((smartpark.model.MotorVehicle) v).getFuelType();
                }
                if (v instanceof Truck) {
                    extra = String.valueOf(((Truck) v).getLoadCapacityTonnes());
                }

                // CSV line formatting
                String line = String.format("%d,%s,%s,%s,%s,%s,%s,%d",
                        ticket.getTicketId(),
                        v.getPlateNumber(),
                        v.getOwnerName(),
                        v.getVehicleType(),
                        fuel,
                        extra,
                        ticket.getParkingSlot().getSlotId(),
                        ticket.getEntryTimeMillis());

                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Loads active tickets from the CSV file into the ParkingLot using BufferedReader.
     * Crucial: Rebuilds slot occupancy and prevents ID collisions by advancing Ticket.idCounter.
     *
     * @param lot target ParkingLot instance
     * @return number of restored active tickets
     * @throws IOException             if file reading fails
     * @throws InvalidVehicleException if parsed records are corrupted
     */
    public int loadActiveTickets(ParkingLot lot) throws IOException, InvalidVehicleException {
        File file = new File(activeTicketsPath);
        if (!file.exists() || file.length() == 0) {
            return 0;
        }

        int restoredCount = 0;
        int highestIdLoaded = 1000;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Split CSV with limit -1 to preserve trailing empty values
                String[] parts = line.split(",", -1);
                if (parts.length < 8) {
                    continue; // Skip malformed line
                }

                int ticketId = Integer.parseInt(parts[0].trim());
                String plate = parts[1].trim();
                String owner = parts[2].trim();
                String type = parts[3].trim();
                String fuel = parts[4].trim();
                String extra = parts[5].trim();
                String slotId = parts[6].trim();
                long entryTime = Long.parseLong(parts[7].trim());

                // Reconstruct polymorphic Vehicle
                Vehicle vehicle;
                if ("Car".equalsIgnoreCase(type)) {
                    vehicle = new Car(plate, owner, fuel.isEmpty() ? "Petrol" : fuel);
                } else if ("Bike".equalsIgnoreCase(type)) {
                    vehicle = new Bike(plate, owner, fuel.isEmpty() ? "Petrol" : fuel);
                } else if ("Truck".equalsIgnoreCase(type)) {
                    double capacity = extra.isEmpty() ? 5.0 : Double.parseDouble(extra);
                    vehicle = new Truck(plate, owner, fuel.isEmpty() ? "Diesel" : fuel, capacity);
                } else {
                    continue; // Unknown vehicle type
                }

                // Locate the matching slot in the lot hierarchy
                ParkingSlot designatedSlot = findSlotById(lot, slotId);
                if (designatedSlot != null && designatedSlot.isAvailable()) {
                    try {
                        designatedSlot.assignVehicle(vehicle);
                        Ticket ticket = new Ticket(ticketId, vehicle, designatedSlot, entryTime, 0L, true);
                        lot.restoreActiveTicket(ticket);
                        restoredCount++;

                        if (ticketId > highestIdLoaded) {
                            highestIdLoaded = ticketId;
                        }
                    } catch (Exception e) {
                        System.err.println("[WARN] Could not restore ticket #" + ticketId + ": " + e.getMessage());
                    }
                }
            }
        }

        // PREVENT ID COLLISION: Forward the static counter to the highest ticketId found!
        Ticket.resetIdCounter(highestIdLoaded);

        return restoredCount;
    }

    private ParkingSlot findSlotById(ParkingLot lot, String slotId) {
        for (Floor floor : lot.getFloors()) {
            for (ParkingSlot slot : floor.getSlots()) {
                if (slot.getSlotId().equalsIgnoreCase(slotId)) {
                    return slot;
                }
            }
        }
        return null;
    }

    // =========================================================================
    // 2. BYTE STREAMS: Binary Append-Only Audit Log (DataOutputStream & DataInputStream)
    // =========================================================================

    /**
     * Appends a closed parking transaction record to the binary audit ledger (.dat).
     * Uses FileOutputStream in append mode wrapped with DataOutputStream.
     *
     * @param closedTicket completed ticket
     * @param feePaid      final settled amount in INR
     * @param paymentMode  channel used (Cash, UPI)
     * @throws IOException if binary write fails
     */
    public void appendClosedTicketLog(Ticket closedTicket, double feePaid, String paymentMode) throws IOException {
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(binaryLogPath, true)))) {
            out.writeInt(closedTicket.getTicketId());
            out.writeUTF(closedTicket.getVehicle().getPlateNumber());
            out.writeUTF(closedTicket.getVehicle().getVehicleType());
            out.writeDouble(feePaid);
            out.writeLong(closedTicket.getExitTimeMillis());
            out.writeUTF(paymentMode);
            out.flush();
        }
    }

    /**
     * DTO representing an archived financial transaction read from the binary ledger.
     */
    public static class AuditLogEntry {
        private final int ticketId;
        private final String plateNumber;
        private final String vehicleType;
        private final double feePaid;
        private final long exitTimeMillis;
        private final String paymentMode;

        public AuditLogEntry(int ticketId, String plateNumber, String vehicleType,
                             double feePaid, long exitTimeMillis, String paymentMode) {
            this.ticketId = ticketId;
            this.plateNumber = plateNumber;
            this.vehicleType = vehicleType;
            this.feePaid = feePaid;
            this.exitTimeMillis = exitTimeMillis;
            this.paymentMode = paymentMode;
        }

        public int getTicketId() { return ticketId; }
        public String getPlateNumber() { return plateNumber; }
        public String getVehicleType() { return vehicleType; }
        public double getFeePaid() { return feePaid; }
        public long getExitTimeMillis() { return exitTimeMillis; }
        public String getPaymentMode() { return paymentMode; }

        @Override
        public String toString() {
            return String.format("LogEntry [Ticket #%d | %s (%s) | Paid: Rs. %.2f via %s | Exit: %d]",
                    ticketId, plateNumber, vehicleType, feePaid, paymentMode, exitTimeMillis);
        }
    }

    /**
     * Reads and parses all historical records from the binary ledger using DataInputStream.
     *
     * @return list of parsed binary log entries
     * @throws IOException if file reading fails
     */
    public List<AuditLogEntry> readBinaryLogs() throws IOException {
        List<AuditLogEntry> logs = new ArrayList<>();
        File file = new File(binaryLogPath);
        if (!file.exists() || file.length() == 0) {
            return logs;
        }

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            while (true) {
                try {
                    int ticketId = in.readInt();
                    String plate = in.readUTF();
                    String type = in.readUTF();
                    double fee = in.readDouble();
                    long exitTime = in.readLong();
                    String mode = in.readUTF();
                    logs.add(new AuditLogEntry(ticketId, plate, type, fee, exitTime, mode));
                } catch (EOFException e) {
                    // Normal end of binary file
                    break;
                }
            }
        }
        return logs;
    }
}
