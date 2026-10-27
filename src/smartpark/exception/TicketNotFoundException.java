package smartpark.exception;

/**
 * Thrown when an operation references a ticket ID that does not exist in
 * the active tickets registry.
 */
public class TicketNotFoundException extends Exception {
    public TicketNotFoundException(String message) {
        super(message);
    }
}
