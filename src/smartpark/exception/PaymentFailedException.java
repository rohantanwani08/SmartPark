package smartpark.exception;

/**
 * Thrown when payment validation or processing fails (e.g. insufficient cash tendered,
 * invalid UPI handle).
 */
public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) {
        super(message);
    }
}
