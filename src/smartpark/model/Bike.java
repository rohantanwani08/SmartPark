package smartpark.model;

import smartpark.exception.InvalidVehicleException;

/**
 * Concrete representation of a two-wheeler / motorcycle.
 * Features specialized hourly parking rate.
 */
public class Bike extends MotorVehicle {
    public static final double HOURLY_RATE = 20.0;

    /**
     * Overloaded constructor defaulting fuel type to "Petrol".
     *
     * @param plateNumber unique vehicle license plate
     * @param ownerName   name of the vehicle owner
     * @throws InvalidVehicleException if fields are invalid
     */
    public Bike(String plateNumber, String ownerName) throws InvalidVehicleException {
        this(plateNumber, ownerName, "Petrol");
    }

    /**
     * Primary constructor specifying explicit fuel type.
     *
     * @param plateNumber unique vehicle license plate
     * @param ownerName   name of the vehicle owner
     * @param fuelType    fuel type (e.g. Petrol, Electric)
     * @throws InvalidVehicleException if fields are invalid
     */
    public Bike(String plateNumber, String ownerName, String fuelType) throws InvalidVehicleException {
        super(plateNumber, ownerName, fuelType);
    }

    @Override
    public String getVehicleType() {
        return "Bike";
    }

    @Override
    public double calculateFee(int hours) {
        int billedHours = Math.max(1, hours);
        return billedHours * HOURLY_RATE;
    }
}
