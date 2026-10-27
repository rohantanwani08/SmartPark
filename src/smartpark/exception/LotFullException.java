package smartpark.exception;

/**
 * Thrown when an entry attempt fails because all slots compatible with the
 * vehicle type are occupied.
 */
public class LotFullException extends Exception {
    public LotFullException(String message) {
        super(message);
    }
}
