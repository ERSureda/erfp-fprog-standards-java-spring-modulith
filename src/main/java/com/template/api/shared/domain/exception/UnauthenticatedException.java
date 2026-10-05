package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Security exception thrown when an operation requires an authenticated caller identity that is missing.
 * <p>
 * Maps to HTTP 401 Unauthorized at perimeter web adapter boundaries.
 * Conforms to DOM-01 and ADR-003.
 */
public class UnauthenticatedException extends BaseException {

    public UnauthenticatedException(String message) {
        super(CommonError.UNAUTHENTICATED, ErrorCategory.UNAUTHENTICATED, message);
    }
}
