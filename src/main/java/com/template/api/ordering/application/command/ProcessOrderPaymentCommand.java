package com.template.api.ordering.application.command;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable command to process an asynchronous order payment event.
 * <p>
 * Dispatched by worker adapters to drive order state transitions upon payment receipt.
 * Enforces fail-fast zero-allocation null checks in its compact constructor.
 * Conforms to APP-02, APP-03, SED-09, and ADR-006.
 *
 * @param orderId       unique identifier of the target order
 * @param paymentStatus payment outcome status (e.g., CONFIRMED, REJECTED)
 */
public record ProcessOrderPaymentCommand(
        UUID orderId,
        String paymentStatus
) {

    public ProcessOrderPaymentCommand {
        Objects.requireNonNull(orderId, "ORDER_ID_CANNOT_BE_NULL");
        Objects.requireNonNull(paymentStatus, "PAYMENT_STATUS_CANNOT_BE_NULL");
    }
}
