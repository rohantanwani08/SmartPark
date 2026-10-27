package smartpark.exception;

/**
 * Thrown when vehicle data is invalid (e.g. empty license plate, negative capacity).
 */
public class InvalidVehicleException extends Exception {
    public InvalidVehicleException(String message) {
        super(message);
    }
}
