package smartpark.model;

import smartpark.exception.InvalidVehicleException;

/**
 * Concrete representation of a passenger car.
 * Demonstrates constructor overloading with constructor chaining using this(...),
 * static constants, and concrete implementation of abstract fee calculation.
 */
public class Car extends MotorVehicle {
    public static final double HOURLY_RATE = 40.0;

    /**
     * Overloaded constructor defaulting fuel type to "Petrol" via constructor chaining.
     *
     * @param plateNumber unique vehicle license plate
     * @param ownerName   name of the vehicle owner
     * @throws InvalidVehicleException if fields are invalid
     */
    public Car(String plateNumber, String ownerName) throws InvalidVehicleException {
        this(plateNumber, ownerName, "Petrol");
    }

    /**
     * Primary constructor specifying explicit fuel type.
     *
     * @param plateNumber unique vehicle license plate
     * @param ownerName   name of the vehicle owner
     * @param fuelType    fuel type (e.g. Petrol, Diesel, Electric)
     * @throws InvalidVehicleException if fields are invalid
     */
    public Car(String plateNumber, String ownerName, String fuelType) throws InvalidVehicleException {
        super(plateNumber, ownerName, fuelType);
    }

    @Override
    public String getVehicleType() {
        return "Car";
    }

    @Override
    public double calculateFee(int hours) {
        int billedHours = Math.max(1, hours);
        return billedHours * HOURLY_RATE;
    }
}
