package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;
import com.template.api.shared.domain.error.ErrorCode;
import com.template.api.shared.domain.exception.BaseException;

import java.io.Serial;

/**
 * Technical exception thrown when an external HTTP client, downstream API, or remote service integration fails.
 */
public class ExternalServiceException extends BaseException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ExternalServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, ErrorCategory.INTERNAL, message, cause);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(CommonError.INTERNAL_ERROR, ErrorCategory.INTERNAL, message, cause);
    }

    public ExternalServiceException(ErrorCode errorCode, String message) {
        super(errorCode, ErrorCategory.INTERNAL, message, null);
    }
}