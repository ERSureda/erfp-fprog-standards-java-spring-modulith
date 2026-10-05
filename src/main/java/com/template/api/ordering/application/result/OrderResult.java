package com.template.api.ordering.application.result;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable application response DTO representing order details.
 * <p>
 * Returned across module boundaries and HTTP interfaces without exposing internal domain entities.
 * Conforms to APP-02 and ADR-005.
 *
 * @param id       unique identifier of the order
 * @param status   current lifecycle status
 * @param amount   monetary amount of the order
 * @param currency ISO-4217 currency code
 */
public record OrderResult(
        UUID id,
        String status,
        BigDecimal amount,
        String currency
) {
}
