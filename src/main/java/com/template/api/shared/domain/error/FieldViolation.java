package com.template.api.shared.domain.error;

/**
 * Immutable representation of a validation failure on a specific property or attribute.
 * <p>
 * Encapsulates the invalid field identifier and its corresponding invariant explanation.
 *
 * @param field   name or path of the rejected property
 * @param message description of the violated validation rule
 */
public record FieldViolation(String field, String message) {}
