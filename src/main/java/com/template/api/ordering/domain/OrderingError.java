package com.template.api.ordering.domain;

import com.template.api.shared.domain.error.ErrorCode;

/**
 * Catalog of typed business error codes for the Ordering bounded context.
 * <p>
 * Defines specific domain error identifiers mapped by web and worker exception handlers.
 * Conforms to DOM-01, ERR-03, and SHR-04.
 */
public enum OrderingError implements ErrorCode {

    ORDER_INVALID_STATUS,
    ORDER_NOT_CONFIRMED,
    ORDER_ALREADY_SHIPPED,
    ORDER_NOT_FOUND,
    INSUFFICIENT_STOCK,
    PAYMENT_GATEWAY_TIMEOUT;

    @Override
    public String code() {
        return name();
    }
}
