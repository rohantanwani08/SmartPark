package smartpark.model;

import smartpark.exception.InvalidVehicleException;

/**
 * Concrete representation of a heavy cargo truck.
 * Features weight-based pricing: adds a 10% heavy load surcharge
 * if load capacity exceeds 10 tonnes.
 */
public class Truck extends MotorVehicle {
    public static final double BASE_RATE = 80.0;
    public static final double HEAVY_LOAD_THRESHOLD_TONNES = 10.0;
    public static final double HEAVY_LOAD_SURCHARGE_MULTIPLIER = 1.10;

    private final double loadCapacityTonnes;

    /**
     * Constructs a Truck with plate, owner, fuel, and load capacity validation.
     *
     * @param plateNumber        unique vehicle license plate
     * @param ownerName          name of the vehicle owner
     * @param fuelType           fuel type (e.g. Diesel, Electric, CNG)
     * @param loadCapacityTonnes cargo capacity in metric tonnes (must be positive)
     * @throws InvalidVehicleException if any field is invalid or load capacity is non-positive
     */
    public Truck(String plateNumber, String ownerName, String fuelType, double loadCapacityTonnes)
            throws InvalidVehicleException {
        super(plateNumber, ownerName, fuelType);
        if (loadCapacityTonnes <= 0.0) {
            throw new InvalidVehicleException("Truck load capacity must be greater than 0 tonnes.");
        }
        this.loadCapacityTonnes = loadCapacityTonnes;
    }

    public double getLoadCapacityTonnes() {
        return loadCapacityTonnes;
    }

    @Override
    public String getVehicleType() {
        return "Truck";
    }

    @Override
    public double calculateFee(int hours) {
        int billedHours = Math.max(1, hours);
        double rate = BASE_RATE;
        if (loadCapacityTonnes > HEAVY_LOAD_THRESHOLD_TONNES) {
            rate *= HEAVY_LOAD_SURCHARGE_MULTIPLIER;
        }
        return billedHours * rate;
    }

    @Override
    public String toString() {
        return super.toString() + " [Capacity=" + loadCapacityTonnes + "T]";
    }
}
