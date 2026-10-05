package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

/**
 * Domain exception indicating an operation conflicts with the current entity state or uniqueness rules.
 * <p>
 * Maps to HTTP 409 Conflict at web adapter boundaries.
 * Conforms to DOM-01 and ADR-003.
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(CommonError.CONFLICT, ErrorCategory.CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.CONFLICT, message);
    }
}
