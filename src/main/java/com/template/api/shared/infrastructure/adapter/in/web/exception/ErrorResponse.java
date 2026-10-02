package com.template.api.shared.infrastructure.adapter.in.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.template.api.shared.domain.error.FieldViolation;

import java.util.List;

/**
 * Immutable standard contract for HTTP error payloads.
 * <p>
 * Suppresses empty violation lists to keep network payloads compact.
 *
 * @param status HTTP numeric status code
 * @param code   unique business or platform error code
 * @param detail human-readable explanation
 * @param errors granular field-level validation issues, omitted if empty
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