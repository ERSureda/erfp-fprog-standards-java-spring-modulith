package com.template.api.ordering.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable application response DTO representing an order.
 */
public record OrderResult(
        UUID id,
        String status,
        BigDecimal amount,
        String currency
) {

    public OrderResult(UUID id, String status, BigDecimal amount) {
        this(id, status, amount, "EUR");
    }
}
