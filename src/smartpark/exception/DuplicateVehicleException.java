package smartpark.exception;

/**
 * Thrown when an entry attempt is made for a vehicle with a license plate
 * that is already registered as actively parked in the system.
 */
public class DuplicateVehicleException extends Exception {
    public DuplicateVehicleException(String message) {
        super(message);
    }
}
