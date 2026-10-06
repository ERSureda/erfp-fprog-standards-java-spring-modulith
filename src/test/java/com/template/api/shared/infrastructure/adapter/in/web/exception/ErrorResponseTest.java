package com.template.api.shared.infrastructure.adapter.in.web.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.shared.domain.error.FieldViolation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorResponse}.
 * <p>
 * Verifies JSON serialization semantics, omission of empty violation lists due to {@code JsonInclude.Include.NON_EMPTY},
 * and canonical record constructor defaults.
 */
@DisplayName("ErrorResponse Unit Tests")
class ErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should omit errors list in JSON payload when errors is empty due to NON_EMPTY inclusion")
    void shouldOmitEmptyErrorsListInJson() throws Exception {
        ErrorResponse response = new ErrorResponse(404, "RESOURCE_NOT_FOUND", "Entity not found");

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\":404");
        assertThat(json).contains("\"code\":\"RESOURCE_NOT_FOUND\"");
        assertThat(json).contains("\"detail\":\"Entity not found\"");
        assertThat(json).doesNotContain("\"errors\"");
    }

    @Test
    @DisplayName("Should serialize errors with field violations when errors is populated")
    void shouldSerializeErrorsWithViolations() throws Exception {
        List<FieldViolation> violations = List.of(
                new FieldViolation("email", "must be a valid email address"),
                new FieldViolation("age", "must be greater than 18")
        );
        ErrorResponse response = new ErrorResponse(400, "VALIDATION_FAILED", "Validation failed", violations);

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"status\":400");
        assertThat(json).contains("\"code\":\"VALIDATION_FAILED\"");
        assertThat(json).contains("\"detail\":\"Validation failed\"");
        assertThat(json).contains("\"field\":\"email\"");
        assertThat(json).contains("\"message\":\"must be a valid email address\"");
        assertThat(json).contains("\"field\":\"age\"");
        assertThat(json).contains("\"message\":\"must be greater than 18\"");
    }

    @Test
    @DisplayName("Record accessors and 3-arg constructor should initialize default empty errors")
    void recordAccessorsAndConstructor() {
        ErrorResponse response = new ErrorResponse(500, "INTERNAL_SERVER_ERROR", "Unexpected error");

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.detail()).isEqualTo("Unexpected error");
        assertThat(response.errors()).isEmpty();
    }
}
