package com.template.api.shared.domain.error;

/**
 * Fallback error codes for generic cross-cutting scenarios.
 * <p>
 * Bounded contexts should define their own specific enums implementing {@link ErrorCode}.
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