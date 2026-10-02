package com.template.api.shared.domain.exception;

import com.template.api.shared.domain.error.CommonError;
import com.template.api.shared.domain.error.ErrorCategory;

/**
 * Thrown when an aggregate root, domain entity, or requested resource cannot be found.
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(CommonError.RESOURCE_NOT_FOUND, ErrorCategory.NOT_FOUND, message);
    }

    public ResourceNotFoundException(Class<?> entityClass, Object id) {
        this("Resource '%s' with id '%s' not found".formatted(
                entityClass != null ? entityClass.getSimpleName() : "Entity", id));
    }
}