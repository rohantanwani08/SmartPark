package smartpark.service;

import smartpark.exception.DuplicateVehicleException;
import smartpark.exception.InvalidVehicleException;
import smartpark.exception.LotFullException;
import smartpark.exception.PaymentFailedException;
import smartpark.exception.TicketNotFoundException;
import smartpark.model.Floor;
import smartpark.model.ParkingSlot;
import smartpark.model.Ticket;
import smartpark.model.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central orchestrator and service facade for the SmartPark system.
 * Manages multi-floor slot allocations, prevents duplicate vehicle admissions,
 * and guarantees transactional ordering during vehicle departure.
 *
 * Adheres strictly to Decoupling: Contains zero calls to System.out or Scanner.
 */
public class ParkingLot {
    private final String lotName;
    private final Floor[] floors;
    private final List<Ticket> activeTickets;
    private final List<Ticket> ticketHistory;
    private double totalRevenue;
    private FileManager fileManager;

    /**
     * Constructs a ParkingLot service instance with designated floors.
     *
     * @param lotName descriptive name of the facility
     * @param floors  array of configured floor levels (Composition)
     * @throws InvalidVehicleException if parameters are null or empty
     */
    public ParkingLot(String lotName, Floor[] floors) throws InvalidVehicleException {
        if (lotName == null || lotName.trim().isEmpty()) {
            throw new InvalidVehicleException("Parking lot name cannot be null or empty.");
        }
        if (floors == null || floors.length == 0) {
            throw new InvalidVehicleException("Parking lot must have at least one floor configured.");
        }
        this.lotName = lotName.trim();
        this.floors = floors;
        this.activeTickets = new ArrayList<>();
        this.ticketHistory = new ArrayList<>();
        this.totalRevenue = 0.0;
        this.fileManager = null;
    }

    public void setFileManager(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    /**
     * Re-inserts an active ticket directly during persistence file reload.
     */
    public void restoreActiveTicket(Ticket ticket) {
        if (ticket != null) {
            this.activeTickets.add(ticket);
        }
    }

    public String getLotName() {
        return lotName;
    }

    public Floor[] getFloors() {
        return floors;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    /**
     * Returns an unmodifiable view of currently active tickets.
     */
    public List<Ticket> getActiveTickets() {
        return Collections.unmodifiableList(activeTickets);
    }

    /**
     * Returns an unmodifiable view of settled ticket history.
     */
    public List<Ticket> getTicketHistory() {
        return Collections.unmodifiableList(ticketHistory);
    }

    /**
     * Admits a vehicle into the parking facility.
     * Enforces duplicate license plate detection and multi-floor slot search.
     *
     * @param vehicle incoming vehicle
     * @return issued admission ticket
     * @throws DuplicateVehicleException if a vehicle with the same plate is already inside
     * @throws LotFullException          if no compatible free slot exists across all floors
     * @throws InvalidVehicleException  if vehicle is null or slot assignment fails
     */
    public Ticket parkVehicle(Vehicle vehicle)
            throws DuplicateVehicleException, LotFullException, InvalidVehicleException {
        if (vehicle == null) {
            throw new InvalidVehicleException("Cannot admit a null vehicle.");
        }

        // 1. Guard against duplicate license plates
        for (Ticket activeTicket : activeTickets) {
            if (activeTicket.getVehicle().getPlateNumber().equalsIgnoreCase(vehicle.getPlateNumber())) {
                throw new DuplicateVehicleException(String.format(
                        "Vehicle with license plate '%s' is already parked under Ticket #%d (Slot: %s).",
                        vehicle.getPlateNumber(), activeTicket.getTicketId(),
                        activeTicket.getParkingSlot().getSlotId()));
            }
        }

        // 2. Search floors sequentially for the first free compatible slot
        ParkingSlot freeSlot = null;
        for (Floor floor : floors) {
            freeSlot = floor.findFreeSlot(vehicle);
            if (freeSlot != null) {
                break;
            }
        }

        // 3. Throw exception if entire facility has no matching free capacity
        if (freeSlot == null) {
            throw new LotFullException("Parking facility is full for vehicle type: " + vehicle.getVehicleType());
        }

        // 4. Assign vehicle to slot, generate timestamped ticket, and register active state
        freeSlot.assignVehicle(vehicle);
        Ticket ticket = new Ticket(vehicle, freeSlot);
        activeTickets.add(ticket);

        // Auto-persist active state to CSV file
        if (fileManager != null) {
            try {
                fileManager.saveActiveTickets(activeTickets);
            } catch (Exception e) {
                System.err.println("[WARN] Failed to auto-persist active tickets: " + e.getMessage());
            }
        }

        return ticket;
    }

    /**
     * Processes vehicle exit with current system time.
     */
    public Ticket exitVehicle(int ticketId, Payment paymentMethod)
            throws TicketNotFoundException, PaymentFailedException, InvalidVehicleException {
        return exitVehicle(ticketId, paymentMethod, System.currentTimeMillis());
    }

    /**
     * Processes vehicle departure with transactional integrity.
     *
     * TRANSACTIONAL ORDER:
     * 1. Validate active ticket existence.
     * 2. Compute billable hours and polymorphic fee.
     * 3. Settle payment via the chosen Strategy (paymentMethod.pay(fee)) FIRST.
     * 4. Only upon successful payment: close ticket, vacate slot, archive to history, and add revenue.
     *    If payment fails, slot and ticket remain untouched!
     *
     * @param ticketId           unique ticket identifier
     * @param paymentMethod      chosen payment strategy (Cash, UPI, etc.)
     * @param exitTimeMillis     epoch millisecond timestamp of departure
     * @return closed ticket with recorded exit details
     * @throws TicketNotFoundException if ticketId is not currently active
     * @throws PaymentFailedException  if payment validation or settlement fails
     * @throws InvalidVehicleException if arguments are invalid
     */
    public Ticket exitVehicle(int ticketId, Payment paymentMethod, long exitTimeMillis)
            throws TicketNotFoundException, PaymentFailedException, InvalidVehicleException {
        if (paymentMethod == null) {
            throw new PaymentFailedException("Payment method cannot be null.");
        }

        // 1. Locate active ticket
        Ticket ticket = findActiveTicketById(ticketId);
        if (ticket == null) {
            throw new TicketNotFoundException("No active ticket found with ID #" + ticketId + ".");
        }

        // 2. Calculate parking duration and polymorphic fee
        int hours = ticket.calculateHoursParked(exitTimeMillis);
        double fee = ticket.getVehicle().calculateFee(hours);

        // 3. TRANSACTIONAL CHECKPOINT: Collect payment BEFORE mutating system state!
        // If payment fails, an exception is thrown immediately and the lot remains intact.
        paymentMethod.pay(fee);

        // 4. State mutation: only executed if payment succeeded without throwing an exception
        ticket.closeTicket(exitTimeMillis);
        ticket.getParkingSlot().vacate();
        activeTickets.remove(ticket);
        ticketHistory.add(ticket);
        totalRevenue += fee;

        // Auto-persist active state and append transaction to binary audit ledger
        if (fileManager != null) {
            try {
                fileManager.saveActiveTickets(activeTickets);
                fileManager.appendClosedTicketLog(ticket, fee, paymentMethod.getMode());
            } catch (Exception e) {
                System.err.println("[WARN] Failed to record transaction in persistence ledger: " + e.getMessage());
            }
        }

        return ticket;
    }

    /**
     * Looks up an active ticket by its unique identifier.
     *
     * @param ticketId identifier to search
     * @return matching active Ticket, or null if not found
     */
    public Ticket findActiveTicketById(int ticketId) {
        for (Ticket ticket : activeTickets) {
            if (ticket.getTicketId() == ticketId) {
                return ticket;
            }
        }
        return null;
    }

    /**
     * Calculates total slot capacity across all floors.
     */
    public int getTotalCapacity() {
        int total = 0;
        for (Floor floor : floors) {
            total += floor.getTotalSlots();
        }
        return total;
    }

    /**
     * Calculates total currently occupied slots across all floors.
     */
    public int getOccupiedSlotsCount() {
        return activeTickets.size();
    }

    /**
     * Calculates total currently available slots across all floors.
     */
    public int getFreeSlotsCount() {
        return getTotalCapacity() - getOccupiedSlotsCount();
    }

    /**
     * Generates a comprehensive formatted status report of the parking facility.
     * Returned as a String to keep the service presentation-agnostic.
     *
     * @return multi-line status report
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        String line = "================================================================";
        sb.append(line).append("\n");
        sb.append(String.format("   STATUS REPORT: %s%n", lotName.toUpperCase()));
        sb.append(line).append("\n");
        sb.append(String.format("Total Capacity   : %d slots%n", getTotalCapacity()));
        sb.append(String.format("Occupied Slots   : %d slots%n", getOccupiedSlotsCount()));
        sb.append(String.format("Available Slots  : %d slots%n", getFreeSlotsCount()));
        sb.append(String.format("Active Vehicles  : %d%n", activeTickets.size()));
        sb.append(String.format("Closed Sessions  : %d%n", ticketHistory.size()));
        sb.append(String.format("Total Revenue    : Rs. %.2f%n", totalRevenue));
        sb.append("----------------------------------------------------------------\n");
        sb.append("FLOOR BREAKDOWN:\n");
        for (Floor floor : floors) {
            sb.append(String.format(" - Floor %d: %d total slots | Free: [Car: %d, Bike: %d, Truck: %d]%n",
                    floor.getFloorNumber(),
                    floor.getTotalSlots(),
                    floor.getFreeCount("Car"),
                    floor.getFreeCount("Bike"),
                    floor.getFreeCount("Truck")));
        }
        sb.append(line);
        return sb.toString();
    }
}
