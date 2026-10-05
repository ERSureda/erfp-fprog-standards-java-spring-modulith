package com.template.api.ordering.infrastructure.adapter.in.worker.dto;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Inbound event message DTO consumed by asynchronous order workers.
 * <p>
 * Transports payment outcome payloads across messaging queues into the application layer.
 * Conforms to INP-04 and TRX-05.
 *
 * @param orderId       unique identifier of the order
 * @param paymentStatus status outcome of the payment process
 * @param tenantId      optional tenant identifier for multi-tenant context binding
 */
public record OrderPaymentEventMessage(
        @NotNull UUID orderId,
        String paymentStatus,
        UUID tenantId
) {

    public OrderPaymentEventMessage(UUID orderId, String paymentStatus) {
        this(orderId, paymentStatus, null);
    }

    public ProcessOrderPaymentCommand toCommand() {
        return new ProcessOrderPaymentCommand(
                this.orderId,
                this.paymentStatus != null ? this.paymentStatus : "CONFIRMED"
        );
    }
}
