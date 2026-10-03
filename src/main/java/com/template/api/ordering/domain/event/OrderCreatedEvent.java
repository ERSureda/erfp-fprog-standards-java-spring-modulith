package com.template.api.ordering.domain.event;

import com.template.api.shared.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable domain event emitted when a new order is created.
 */
public record OrderCreatedEvent(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String eventType,
        UUID customerId,
        BigDecimal amount,
        String currency
) implements DomainEvent {

    public static final String EVENT_TYPE = "ordering.order.created.v1";

    public static OrderCreatedEvent of(UUID orderId, UUID customerId, BigDecimal amount, String currency) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                orderId.toString(),
                Instant.now(),
                EVENT_TYPE,
                customerId,
                amount,
                currency
        );
    }
}
