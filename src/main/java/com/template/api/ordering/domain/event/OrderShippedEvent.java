package com.template.api.ordering.domain.event;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.UUID;

/**
 * Immutable domain event emitted when an order transitions to shipped status.
 * <p>
 * Signals fulfillment dispatch to logistics and notification listeners.
 * Conforms to DOM-01, DOM-05, and TRX-03.
 *
 * @param orderId    unique identifier of the shipped order
 * @param customerId unique identifier of the ordering customer
 */
public record OrderShippedEvent(
        UUID orderId,
        UUID customerId
) implements DomainEvent {

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
