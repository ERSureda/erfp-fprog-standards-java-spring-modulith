package com.template.api.ordering.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable command requesting the creation of a new order.
 * <p>
 * Carries customer identity and order monetary attributes into the application boundary.
 * Conforms to APP-02 and ADR-006.
 *
 * @param customerId optional explicit customer identifier, or null when inferred from execution context
 * @param amount     monetary total of the requested order
 * @param currency   ISO-4217 currency representation
 */
public record CreateOrderCommand(
        UUID customerId,
        BigDecimal amount,
        String currency
) {

    public CreateOrderCommand(BigDecimal amount, String currency) {
        this(null, amount, currency);
    }
}
