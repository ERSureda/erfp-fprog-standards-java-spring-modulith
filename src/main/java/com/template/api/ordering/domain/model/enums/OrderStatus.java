package com.template.api.ordering.domain.model.enums;

/**
 * Lifecycle states of an Order Aggregate Root.
 * <p>
 * Represents state transitions from creation to final fulfillment or cancellation.
 * Conforms to DOM-01 and ADR-005.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    CANCELLED
}
