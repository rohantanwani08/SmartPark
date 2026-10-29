package smartpark.service;

import smartpark.exception.PaymentFailedException;

/**
 * Concrete Unified Payments Interface (UPI) payment strategy.
 * Validates Virtual Payment Address (VPA) syntax and simulates
 * instant digital settlement with transaction reference generation.
 */
public class UPIPayment implements Payment {
    private final String upiId;
    private String transactionRef;
    private boolean completed;

    /**
     * Constructs a UPIPayment strategy with the customer's UPI VPA.
     *
     * @param upiId virtual payment address (e.g. "rohan@okhdfcbank")
     * @throws PaymentFailedException if upiId is blank, null, or lacks valid '@' syntax
     */
    public UPIPayment(String upiId) throws PaymentFailedException {
        if (upiId == null || upiId.trim().isEmpty()) {
            throw new PaymentFailedException("UPI ID cannot be null or empty.");
        }
        String trimmed = upiId.trim();
        int atIndex = trimmed.indexOf('@');
        // VPA must contain '@' not at the start and not at the end
        if (atIndex <= 0 || atIndex >= trimmed.length() - 1) {
            throw new PaymentFailedException("Invalid UPI ID format: '" + trimmed + "'. Expected format: username@bank");
        }
        this.upiId = trimmed;
        this.transactionRef = null;
        this.completed = false;
    }

    public String getUpiId() {
        return upiId;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String getMode() {
        return "UPI";
    }

    @Override
    public void pay(double amount) throws PaymentFailedException {
        if (amount <= 0.0) {
            throw new PaymentFailedException("Billed amount must be greater than zero.");
        }
        // Simulate digital gateway approval & generate unique reference ID
        long timestamp = System.currentTimeMillis();
        int randomSuffix = (int) (Math.random() * 9000) + 1000;
        this.transactionRef = "UPI-TXN-" + timestamp + "-" + randomSuffix;
        this.completed = true;
    }

    @Override
    public String toString() {
        if (!completed) {
            return String.format("UPIPayment [VPA: %s | Status: PENDING]", upiId);
        }
        return String.format("UPIPayment [VPA: %s | TxnRef: %s | Status: SUCCESS]", upiId, transactionRef);
    }
}
