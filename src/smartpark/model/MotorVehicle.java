package smartpark.model;

import smartpark.exception.InvalidVehicleException;

/**
 * Intermediate abstract class representing motorized vehicles that consume fuel or electricity.
 * Inherits core identity from Vehicle and introduces fuel classification.
 */
public abstract class MotorVehicle extends Vehicle {
    private final String fuelType;

    /**
     * Constructs a MotorVehicle.
     *
     * @param plateNumber unique vehicle license plate
     * @param ownerName   name of the vehicle owner
     * @param fuelType    fuel type (e.g. Petrol, Diesel, Electric, CNG)
     * @throws InvalidVehicleException if validation fails
     */
    public MotorVehicle(String plateNumber, String ownerName, String fuelType) throws InvalidVehicleException {
        super(plateNumber, ownerName);
        if (fuelType == null || fuelType.trim().isEmpty()) {
            throw new InvalidVehicleException("Fuel type cannot be null or empty.");
        }
        this.fuelType = fuelType.trim();
    }

    public String getFuelType() {
        return fuelType;
    }

    public boolean isElectric() {
        return "Electric".equalsIgnoreCase(fuelType) || "EV".equalsIgnoreCase(fuelType);
    }

    @Override
    public String toString() {
        return super.toString() + " (Fuel=" + fuelType + ")";
    }
}
