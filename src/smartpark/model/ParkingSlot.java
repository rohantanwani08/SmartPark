package smartpark.model;

import smartpark.exception.InvalidVehicleException;
import smartpark.exception.LotFullException;

/**
 * Represents a single designated parking slot within the parking facility.
 * Demonstrates aggregation (holds a temporary reference to a Vehicle)
 * and strict encapsulation over slot occupancy state.
 */
public class ParkingSlot {
    private final String slotId;
    private final String supportedType;
    private Vehicle currentVehicle;

    /**
     * Constructs an unoccupied ParkingSlot with an identifier and supported vehicle category.
     *
     * @param slotId        unique slot label (e.g. "F1-CAR-01")
     * @param supportedType category of vehicle accepted (e.g. "Car", "Bike", "Truck")
     * @throws InvalidVehicleException if parameters are null or blank
     */
    public ParkingSlot(String slotId, String supportedType) throws InvalidVehicleException {
        if (slotId == null || slotId.trim().isEmpty()) {
            throw new InvalidVehicleException("Slot ID cannot be null or empty.");
        }
        if (supportedType == null || supportedType.trim().isEmpty()) {
            throw new InvalidVehicleException("Supported vehicle type cannot be null or empty.");
        }
        this.slotId = slotId.trim().toUpperCase();
        this.supportedType = supportedType.trim();
        this.currentVehicle = null;
    }

    public String getSlotId() {
        return slotId;
    }

    public String getSupportedType() {
        return supportedType;
    }

    public Vehicle getCurrentVehicle() {
        return currentVehicle;
    }

    /**
     * Checks if this slot is currently free.
     *
     * @return true if unoccupied, false otherwise
     */
    public boolean isAvailable() {
        return currentVehicle == null;
    }

    /**
     * Checks whether a specific vehicle can fit into this slot.
     * A vehicle fits only if the slot is free and matches the vehicle's category.
     *
     * @param vehicle vehicle to inspect
     * @return true if compatible and free
     */
    public boolean canFit(Vehicle vehicle) {
        if (!isAvailable() || vehicle == null) {
            return false;
        }
        return supportedType.equalsIgnoreCase(vehicle.getVehicleType());
    }

    /**
     * Assigns a vehicle to this slot.
     *
     * @param vehicle incoming vehicle
     * @throws LotFullException        if the slot is already occupied
     * @throws InvalidVehicleException if vehicle is null or incompatible
     */
    public void assignVehicle(Vehicle vehicle) throws LotFullException, InvalidVehicleException {
        if (!isAvailable()) {
            throw new LotFullException("Slot " + slotId + " is already occupied by " + currentVehicle.getPlateNumber() + ".");
        }
        if (vehicle == null) {
            throw new InvalidVehicleException("Cannot assign null vehicle to slot " + slotId + ".");
        }
        if (!supportedType.equalsIgnoreCase(vehicle.getVehicleType())) {
            throw new InvalidVehicleException("Cannot park " + vehicle.getVehicleType() + " in slot designated for " + supportedType + ".");
        }
        this.currentVehicle = vehicle;
    }

    /**
     * Vacates the slot, detaching the current vehicle reference and resetting state.
     *
     * @return the departing vehicle, or null if the slot was already empty
     */
    public Vehicle vacate() {
        Vehicle departedVehicle = this.currentVehicle;
        this.currentVehicle = null;
        return departedVehicle;
    }

    @Override
    public String toString() {
        return String.format("Slot[%s | Type=%-5s | Status=%s]",
                slotId,
                supportedType,
                isAvailable() ? "FREE" : "OCCUPIED (" + currentVehicle.getPlateNumber() + ")");
    }
}
