package com.template.api.ordering.domain;

import com.template.api.shared.domain.error.ErrorCode;

/**
 * Catalog of typed business error codes for the Ordering bounded context.
 * <p>
 * Defines specific domain error identifiers mapped by web and worker exception handlers.
 * Conforms to DOM-01, ERR-03, and SHR-04.
 */
public enum OrderingError implements ErrorCode {

    ORDERING_ORDER_INVALID_STATUS,
    ORDERING_ORDER_NOT_CONFIRMED,
    ORDERING_ORDER_ALREADY_SHIPPED,
    ORDERING_ORDER_NOT_FOUND,
    ORDERING_STOCK_INSUFFICIENT,
    ORDERING_PAYMENT_GATEWAY_TIMEOUT;

    @Override
    public String code() {
        return name();
    }
}
