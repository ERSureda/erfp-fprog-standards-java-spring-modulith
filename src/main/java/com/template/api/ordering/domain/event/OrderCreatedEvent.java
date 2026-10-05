package com.template.api.ordering.domain.event;

import com.template.api.shared.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable domain event emitted when a new order is created.
 * Encapsulates purely business-relevant payload data.
 * Conforms to DOM-01 and TRX-03.
 */
public record OrderCreatedEvent(
        UUID orderId,
        UUID customerId,
        BigDecimal amount,
        String currency
) implements DomainEvent {

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
