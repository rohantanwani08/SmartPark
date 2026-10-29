package smartpark.service;

import smartpark.exception.PaymentFailedException;

/**
 * Concrete cash payment strategy.
 * Handles physical tender validation, exact match or change calculation,
 * and ensures payment fails if tendered cash is insufficient.
 */
public class CashPayment implements Payment {
    private final double cashTendered;
    private double changeGiven;
    private boolean completed;

    /**
     * Constructs a CashPayment strategy with the cash offered by the driver.
     *
     * @param cashTendered amount of physical currency handed over (must be positive)
     * @throws PaymentFailedException if cashTendered <= 0
     */
    public CashPayment(double cashTendered) throws PaymentFailedException {
        if (cashTendered <= 0.0) {
            throw new PaymentFailedException("Cash tendered must be greater than zero.");
        }
        this.cashTendered = cashTendered;
        this.changeGiven = 0.0;
        this.completed = false;
    }

    public double getCashTendered() {
        return cashTendered;
    }

    public double getChangeGiven() {
        return changeGiven;
    }

    public boolean isCompleted() {
        return completed;
    }

    @Override
    public String getMode() {
        return "Cash";
    }

    @Override
    public void pay(double amount) throws PaymentFailedException {
        if (amount <= 0.0) {
            throw new PaymentFailedException("Billed amount must be greater than zero.");
        }
        if (cashTendered < amount) {
            throw new PaymentFailedException(String.format(
                    "Insufficient cash tendered. Required: Rs. %.2f, Tendered: Rs. %.2f (Short by Rs. %.2f)",
                    amount, cashTendered, (amount - cashTendered)));
        }
        this.changeGiven = cashTendered - amount;
        this.completed = true;
    }

    @Override
    public String toString() {
        if (!completed) {
            return String.format("CashPayment [Tendered: Rs. %.2f | Status: PENDING]", cashTendered);
        }
        return String.format("CashPayment [Tendered: Rs. %.2f | Change Returned: Rs. %.2f | Status: SUCCESS]",
                cashTendered, changeGiven);
    }
}
