package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Thrown when an authenticated actor lacks permissions to execute an application use case.
 */
public class ForbiddenException extends BaseException {

    public ForbiddenException(String message) {
        super(CommonError.FORBIDDEN, ErrorCategory.FORBIDDEN, message);
    }
}