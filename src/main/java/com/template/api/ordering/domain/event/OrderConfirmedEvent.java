package com.template.api.ordering.domain.event;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.UUID;

/**
 * Immutable domain event emitted when an order payment is confirmed.
 * <p>
 * Signifies successful payment processing and readiness for inventory shipment.
 * Conforms to DOM-01, DOM-05, and TRX-03.
 *
 * @param orderId    unique identifier of the confirmed order
 * @param customerId unique identifier of the ordering customer
 */
public record OrderConfirmedEvent(
        UUID orderId,
        UUID customerId
) implements DomainEvent {

    @Override
    public String aggregateId() {
        return orderId.toString();
    }
}
