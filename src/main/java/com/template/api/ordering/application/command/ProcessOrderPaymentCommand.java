package com.template.api.ordering.application.command;

import java.util.UUID;

/**
 * Immutable command to process an asynchronous order payment event.
 * <p>
 * Dispatched by worker adapters to drive order state transitions upon payment receipt.
 * Conforms to APP-02 and TRX-05.
 *
 * @param orderId       unique identifier of the target order
 * @param paymentStatus payment outcome status (e.g., CONFIRMED, REJECTED)
 */
public record ProcessOrderPaymentCommand(
        UUID orderId,
        String paymentStatus
) {
}
