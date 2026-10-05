package com.template.api.ordering.domain.event;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.UUID;

/**
 * Immutable domain event emitted when an order is cancelled.
 * Conforms to DOM-01 and TRX-03.
 */
public record OrderCancelledEvent(
        UUID orderId,
        UUID customerId
) implements DomainEvent {

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
