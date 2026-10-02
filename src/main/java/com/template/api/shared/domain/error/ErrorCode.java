package com.template.api.shared.domain.error;

/**
 * Common contract for unique error identifiers across all modules.
 * <p>
 * Intended to be implemented by domain enums via their natural {@code name()} method.
 */
public interface ErrorCode {

    String code();
}