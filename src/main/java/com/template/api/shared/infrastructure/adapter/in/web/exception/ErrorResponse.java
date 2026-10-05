package com.template.api.shared.infrastructure.adapter.in.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.template.api.shared.domain.error.FieldViolation;

import java.util.List;

/**
 * Standard immutable contract for HTTP error response payloads.
 * <p>
 * Omit empty violation lists to keep network responses compact.
 * Conforms to ADR-003.
 *
 * @param status HTTP numeric status code
 * @param code   typed error code identifier
 * @param detail human-readable explanation
 * @param errors granular field-level validation issues
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
        int status,
        String code,
        String detail,
        List<FieldViolation> errors
) {

    public ErrorResponse(int status, String code, String detail) {
        this(status, code, detail, List.of());
    }
}
