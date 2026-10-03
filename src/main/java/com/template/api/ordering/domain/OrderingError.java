package com.template.api.ordering.domain;

import com.template.api.shared.domain.error.ErrorCode;

/**
 * Closed catalog of business error codes for the ordering bounded context.
 */
public enum OrderingError implements ErrorCode {
    ORDER_NOT_FOUND,
    ORDER_ALREADY_SHIPPED,
    INSUFFICIENT_STOCK,
    PAYMENT_GATEWAY_TIMEOUT;

    @Override
    public String code() {
        return name();
    }
}
