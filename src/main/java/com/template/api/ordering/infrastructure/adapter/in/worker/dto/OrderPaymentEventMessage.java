package com.template.api.ordering.infrastructure.adapter.in.worker.dto;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Inbound asynchronous event message payload consumed by the worker.
 * Exposes a zero-overhead factory method to map directly to the application command.
 */
public record OrderPaymentEventMessage(
        @NotNull UUID orderId,
        String paymentStatus,
        UUID tenantId
) {

    public OrderPaymentEventMessage(UUID orderId, String paymentStatus) {
        this(orderId, paymentStatus, null);
    }

    /**
     * Converts this event message payload into an immutable application command.
     *
     * @return an immutable {@link ProcessOrderPaymentCommand}
     */
    public ProcessOrderPaymentCommand toCommand() {
        return new ProcessOrderPaymentCommand(
                this.orderId,
                this.paymentStatus != null ? this.paymentStatus : "CONFIRMED"
        );
    }
}
