package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Technical exception wrapping unexpected persistence, network, or low-level infrastructure failures.
 * <p>
 * Captures full diagnostic stack traces under the {@link ErrorCategory#INTERNAL} category.
 * Conforms to DOM-01 and ADR-003.
 */
public class InfrastructureException extends BaseException {

    public InfrastructureException(String message, Throwable cause) {
        super(CommonError.INTERNAL_ERROR, ErrorCategory.INTERNAL, message, cause);
    }

    public InfrastructureException(String message) {
        this(message, null);
    }
}
