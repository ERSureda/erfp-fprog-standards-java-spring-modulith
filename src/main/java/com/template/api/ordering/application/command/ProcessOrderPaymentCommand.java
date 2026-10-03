package com.template.api.ordering.application.command;

import java.util.UUID;

/**
 * Immutable command to process an asynchronous order payment event.
 */
public record ProcessOrderPaymentCommand(
        UUID orderId,
        String paymentStatus
) {
}
