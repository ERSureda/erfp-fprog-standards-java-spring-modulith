package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Thrown when an operation strictly requires an authenticated actor identity that is missing.
 */
public class UnauthenticatedException extends BaseException {

    public UnauthenticatedException(String message) {
        super(CommonError.UNAUTHENTICATED, ErrorCategory.UNAUTHENTICATED, message);
    }
}