package com.template.api.shared.domain.error;

/**
 * Immutable representation of a validation failure on a specific property or attribute.
 *
 * @param field   name of the invalid field or property path
 * @param message explanation of the broken validation invariant
 */
public record FieldViolation(String field, String message) {}