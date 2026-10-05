package com.template.api.shared.domain.error;

/**
 * Standard cross-cutting error codes for generic platform scenarios.
 * <p>
 * Provides fallback error representations when specific bounded context error codes are not applicable.
 * Conforms to DOM-01 and ERR-03.
 */
public enum CommonError implements ErrorCode {

    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    CONFLICT,
    UNAUTHENTICATED,
    FORBIDDEN,
    INTERNAL_ERROR;

    @Override
    public String code() {
        return name();
    }
}
