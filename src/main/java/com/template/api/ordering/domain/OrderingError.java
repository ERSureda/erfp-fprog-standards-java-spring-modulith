package com.template.api.ordering.domain;

import com.template.api.shared.domain.error.ErrorCode;

/**
 * Closed catalog of business error codes for the ordering bounded context.
 * Conforms to DOM-01, ERR-03, and SHR-04.
 */
public enum OrderingError implements ErrorCode {

    // --- State Transitions & Invariants (Conflict 409) ---
    ORDER_INVALID_STATUS,
    ORDER_NOT_CONFIRMED,
    ORDER_ALREADY_SHIPPED,

    // --- Query & Resource Lookups (Not Found 404) ---
    ORDER_NOT_FOUND,

    // --- Business & Integration Scenarios ---
    INSUFFICIENT_STOCK,
    PAYMENT_GATEWAY_TIMEOUT;

    @Override
    public String code() {
        return name();
    }
}
