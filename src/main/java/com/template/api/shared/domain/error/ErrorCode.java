package com.template.api.shared.domain.error;

/**
 * Contract for strongly typed business and platform error codes.
 * <p>
 * Implemented by domain error enumerations across bounded contexts to ensure consistent cataloging.
 * Conforms to DOM-01 and ERR-03.
 */
public interface ErrorCode {

    String code();
}
