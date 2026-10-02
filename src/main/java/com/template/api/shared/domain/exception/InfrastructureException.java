package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Technical exception wrapping unexpected persistence, network, or external infrastructure failures.
 */
public class InfrastructureException extends BaseException {

    public InfrastructureException(String message, Throwable cause) {
        super(CommonError.INTERNAL_ERROR, ErrorCategory.INTERNAL, message, cause);
    }

    public InfrastructureException(String message) {
        this(message, null);
    }
}