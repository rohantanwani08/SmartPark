package smartpark.model;

import smartpark.exception.InvalidVehicleException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents an official parking admission ticket.
 * Demonstrates:
 * 1. Static field tracking (shared unique auto-incrementing ID counter).
 * 2. Association with Vehicle and ParkingSlot.
 * 3. Exact duration calculation rounded up to the nearest full hour (minimum 1 hour).
 */
public class Ticket {
    private static int idCounter = 1000;

    private final int ticketId;
    private final Vehicle vehicle;
    private final ParkingSlot slot;
    private final long entryTimeMillis;
    private long exitTimeMillis;
    private boolean active;

    /**
     * Issues a new active Ticket with an auto-incremented ID and current timestamp.
     *
     * @param vehicle vehicle entering the lot
     * @param slot    assigned parking slot
     * @throws InvalidVehicleException if vehicle or slot is null
     */
    public Ticket(Vehicle vehicle, ParkingSlot slot) throws InvalidVehicleException {
        if (vehicle == null) {
            throw new InvalidVehicleException("Cannot generate ticket for null vehicle.");
        }
        if (slot == null) {
            throw new InvalidVehicleException("Cannot generate ticket without an assigned slot.");
        }
        this.ticketId = ++idCounter;
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTimeMillis = System.currentTimeMillis();
        this.exitTimeMillis = 0L;
        this.active = true;
    }

    /**
     * Overloaded constructor for manual simulation, unit testing, and file persistence reload.
     *
     * @param ticketId        explicit ticket identifier
     * @param vehicle         associated vehicle
     * @param slot            associated slot
     * @param entryTimeMillis entry epoch timestamp
     * @param exitTimeMillis  exit epoch timestamp (0 if still active)
     * @param active          true if actively parked, false if closed
     * @throws InvalidVehicleException if parameters are invalid
     */
    public Ticket(int ticketId, Vehicle vehicle, ParkingSlot slot, long entryTimeMillis,
                  long exitTimeMillis, boolean active) throws InvalidVehicleException {
        if (vehicle == null || slot == null) {
            throw new InvalidVehicleException("Vehicle and slot references cannot be null.");
        }
        this.ticketId = ticketId;
        this.vehicle = vehicle;
        this.slot = slot;
        this.entryTimeMillis = entryTimeMillis;
        this.exitTimeMillis = exitTimeMillis;
        this.active = active;
        // Keep the static counter ahead of any reloaded ticket IDs to prevent collisions
        if (ticketId >= idCounter) {
            idCounter = ticketId;
        }
    }

    /**
     * Returns the next ID that will be assigned by the auto-increment counter.
     */
    public static int getNextExpectedId() {
        return idCounter + 1;
    }

    /**
     * Resets or forwards the static ID counter. Essential for Phase 7 file persistence reload.
     *
     * @param nextId new baseline counter value
     */
    public static void resetIdCounter(int nextId) {
        idCounter = nextId;
    }

    public int getTicketId() {
        return ticketId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public ParkingSlot getParkingSlot() {
        return slot;
    }

    public long getEntryTimeMillis() {
        return entryTimeMillis;
    }

    public long getExitTimeMillis() {
        return exitTimeMillis;
    }

    public boolean isActive() {
        return active;
    }

    /**
     * Closes the ticket by setting the exit timestamp and marking it inactive.
     *
     * @param exitTimeMillis time of departure
     */
    public void closeTicket(long exitTimeMillis) {
        this.exitTimeMillis = exitTimeMillis;
        this.active = false;
    }

    /**
     * Calculates billed parking hours based on the exit time.
     * Rule: minimum 1 hour, fractions rounded up to next full hour.
     *
     * @param exitTime departure epoch timestamp
     * @return billable hours (always >= 1)
     */
    public int calculateHoursParked(long exitTime) {
        long durationMillis = Math.max(0, exitTime - entryTimeMillis);
        // 1 hour = 3,600,000 milliseconds
        double rawHours = durationMillis / 3600000.0;
        return Math.max(1, (int) Math.ceil(rawHours));
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String entryStr = sdf.format(new Date(entryTimeMillis));
        String statusStr = active ? "ACTIVE" : "CLOSED at " + sdf.format(new Date(exitTimeMillis));
        return String.format("Ticket #%d [%s | Slot: %s | In: %s | Status: %s]",
                ticketId, vehicle.getPlateNumber(), slot.getSlotId(), entryStr, statusStr);
    }
}
