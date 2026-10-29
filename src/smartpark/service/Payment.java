package smartpark.service;

import smartpark.exception.PaymentFailedException;

/**
 * Strategy interface defining payment processing capability.
 * Follows the Strategy Design Pattern and Interface Segregation Principle:
 * decouples the core parking lot orchestration from concrete payment
 * settlement mechanisms (e.g. Cash, UPI, Card).
 */
public interface Payment {

    /**
     * Executes payment processing for the specified fee amount.
     *
     * @param amount fee amount in INR to settle (must be > 0)
     * @throws PaymentFailedException if validation fails, cash is insufficient, or gateway rejects
     */
    void pay(double amount) throws PaymentFailedException;

    /**
     * Returns the human-readable payment channel description (e.g. "Cash", "UPI").
     *
     * @return payment mode name
     */
    String getMode();
}
