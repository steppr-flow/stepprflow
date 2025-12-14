package io.stepprflow.sample.exception;

/**
 * Exception thrown when there is insufficient inventory to fulfill an order.
 */
public class InsufficientInventoryException extends RuntimeException {

    /** The product ID with insufficient inventory. */
    private final String productId;

    /** The quantity requested. */
    private final int requestedQuantity;

    /**
     * Constructs a new insufficient inventory exception.
     *
     * @param productId the product ID
     * @param requestedQuantity the quantity requested
     */
    public InsufficientInventoryException(final String productId, final int requestedQuantity) {
        super(String.format("Insufficient inventory for product %s (requested: %d)",
                           productId, requestedQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
    }

    /**
     * Constructs a new insufficient inventory exception with available quantity.
     *
     * @param productId the product ID
     * @param requestedQuantity the quantity requested
     * @param availableQuantity the quantity available
     */
    public InsufficientInventoryException(
            final String productId,
            final int requestedQuantity,
            final int availableQuantity) {
        super(String.format("Insufficient inventory for product %s (requested: %d, available: %d)",
                           productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
    }

    /**
     * Returns the product ID with insufficient inventory.
     *
     * @return the product ID
     */
    public String getProductId() {
        return productId;
    }

    /**
     * Returns the quantity that was requested.
     *
     * @return the requested quantity
     */
    public int getRequestedQuantity() {
        return requestedQuantity;
    }
}
