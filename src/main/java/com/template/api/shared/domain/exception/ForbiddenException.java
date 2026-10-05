package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Security exception thrown when an authenticated actor lacks required permissions or roles.
 * <p>
 * Maps to HTTP 403 Forbidden at perimeter web adapter boundaries.
 * Conforms to DOM-01 and ADR-003.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(CommonError.FORBIDDEN, ErrorCategory.FORBIDDEN, message);
    }
}
