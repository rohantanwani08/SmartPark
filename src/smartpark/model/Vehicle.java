package smartpark.model;

import smartpark.exception.InvalidVehicleException;

/**
 * Abstract base class representing any vehicle in the SmartPark system.
 * Enforces shared attributes (license plate, owner name) and contracts for
 * vehicle classification and polymorphic fee calculation.
 */
public abstract class Vehicle {
    private final String plateNumber;
    private final String ownerName;

    /**
     * Constructs a Vehicle with plate validation.
     *
     * @param plateNumber unique vehicle license plate
     * @param ownerName   name of the vehicle owner
     * @throws InvalidVehicleException if plateNumber or ownerName is null or blank
     */
    public Vehicle(String plateNumber, String ownerName) throws InvalidVehicleException {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            throw new InvalidVehicleException("License plate number cannot be null or empty.");
        }
        if (ownerName == null || ownerName.trim().isEmpty()) {
            throw new InvalidVehicleException("Owner name cannot be null or empty.");
        }
        this.plateNumber = plateNumber.trim().toUpperCase();
        this.ownerName = ownerName.trim();
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    /**
     * Returns the human-readable vehicle category (e.g. "Car", "Bike", "Truck").
     */
    public abstract String getVehicleType();

    /**
     * Calculates the parking fee for the given parking duration in hours.
     * Dynamically dispatched at runtime to concrete vehicle types.
     *
     * @param hours billed duration in hours (minimum 1)
     * @return total fee amount in INR
     */
    public abstract double calculateFee(int hours);

    @Override
    public String toString() {
        return getVehicleType() + " [Plate=" + plateNumber + ", Owner=" + ownerName + "]";
    }
}
