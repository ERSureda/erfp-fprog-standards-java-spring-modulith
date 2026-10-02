package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;

/**
 * Thrown when a business operation conflicts with the current aggregate state or uniqueness rules.
 */
public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(CommonError.CONFLICT, ErrorCategory.CONFLICT, message);
    }

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.CONFLICT, message);
    }
}