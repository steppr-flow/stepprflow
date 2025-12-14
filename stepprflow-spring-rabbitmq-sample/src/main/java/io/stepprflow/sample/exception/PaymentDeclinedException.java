package io.stepprflow.sample.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when a payment is declined.
 */
public class PaymentDeclinedException extends RuntimeException {

    /** The amount that was attempted to charge. */
    private final BigDecimal amount;

    /** The reason for the decline. */
    private final String reason;

    /**
     * Constructs a new payment declined exception.
     *
     * @param reason the reason for the decline
     * @param amount the amount that was attempted
     */
    public PaymentDeclinedException(final String reason, final BigDecimal amount) {
        super(String.format("Payment declined: %s (amount: %s)", reason, amount));
        this.reason = reason;
        this.amount = amount;
    }

    /**
     * Constructs a new payment declined exception with cause.
     *
     * @param reason the reason for the decline
     * @param amount the amount that was attempted
     * @param cause the cause of this exception
     */
    public PaymentDeclinedException(
            final String reason,
            final BigDecimal amount,
            final Throwable cause) {
        super(String.format("Payment declined: %s (amount: %s)", reason, amount), cause);
        this.reason = reason;
        this.amount = amount;
    }

    /**
     * Returns the amount that was attempted to charge.
     *
     * @return the amount
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Returns the reason for the decline.
     *
     * @return the decline reason
     */
    public String getReason() {
        return reason;
    }
}
