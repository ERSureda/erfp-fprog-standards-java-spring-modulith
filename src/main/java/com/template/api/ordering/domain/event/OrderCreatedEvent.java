package com.template.api.ordering.domain.event;

import com.template.api.shared.domain.event.DomainEvent;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable domain event emitted when a new Order Aggregate is created.
 * <p>
 * Contains business payload data required by downstream event subscribers.
 * Conforms to DOM-01, DOM-05, and TRX-03.
 *
 * @param orderId    unique identifier of the created order
 * @param customerId unique identifier of the customer placing the order
 * @param amount     monetary total of the order
 * @param currency   ISO-4217 currency code of the order amount
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
