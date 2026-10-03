package com.template.api.ordering.application.command;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable command requesting the creation of a new order.
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
