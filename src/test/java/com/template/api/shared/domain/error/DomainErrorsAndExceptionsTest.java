package com.template.api.shared.domain.error;

import com.template.api.shared.domain.exception.BaseException;
import com.template.api.shared.domain.exception.ConflictException;
import com.template.api.shared.domain.exception.ExternalServiceException;
import com.template.api.shared.domain.exception.ForbiddenException;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.domain.exception.ResourceNotFoundException;
import com.template.api.shared.domain.exception.UnauthenticatedException;
import com.template.api.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Domain Errors and Exceptions Unit Tests")
class DomainErrorsAndExceptionsTest {

    @ParameterizedTest
    @EnumSource(CommonError.class)
    @DisplayName("CommonError codes should match enum constant names")
    void commonErrorCodeMatchesName(CommonError error) {
        assertThat(error.code()).isEqualTo(error.name());
    }

    @Test
    @DisplayName("ErrorCategory should correctly flag diagnostic capture")
    void errorCategory_capturesDiagnostics() {
        assertThat(ErrorCategory.VALIDATION.capturesDiagnostics()).isFalse();
        assertThat(ErrorCategory.NOT_FOUND.capturesDiagnostics()).isFalse();
        assertThat(ErrorCategory.CONFLICT.capturesDiagnostics()).isFalse();
        assertThat(ErrorCategory.UNAUTHENTICATED.capturesDiagnostics()).isFalse();
        assertThat(ErrorCategory.FORBIDDEN.capturesDiagnostics()).isFalse();
        assertThat(ErrorCategory.INTERNAL.capturesDiagnostics()).isTrue();
    }

    @Test
    @DisplayName("FieldViolation record should store and expose field and message")
    void fieldViolation_attributes() {
        FieldViolation violation = new FieldViolation("email", "must be a valid email address");
        assertThat(violation.field()).isEqualTo("email");
        assertThat(violation.message()).isEqualTo("must be a valid email address");
    }

    @Test
    @DisplayName("Business exceptions should suppress stack traces for zero overhead")
    void businessExceptions_shouldSuppressStackTrace() {
        ResourceNotFoundException notFound = new ResourceNotFoundException("Not found");
        assertThat(notFound.getStackTrace()).isEmpty();
        assertThat(notFound.category()).isEqualTo(ErrorCategory.NOT_FOUND);
        assertThat(notFound.errorCode()).isEqualTo(CommonError.RESOURCE_NOT_FOUND);

        ConflictException conflict = new ConflictException("Conflict");
        assertThat(conflict.getStackTrace()).isEmpty();
        assertThat(conflict.category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(conflict.errorCode()).isEqualTo(CommonError.CONFLICT);

        ValidationException validation = new ValidationException("Invalid");
        assertThat(validation.getStackTrace()).isEmpty();
        assertThat(validation.category()).isEqualTo(ErrorCategory.VALIDATION);
        assertThat(validation.errorCode()).isEqualTo(CommonError.VALIDATION_ERROR);

        ForbiddenException forbidden = new ForbiddenException("Forbidden");
        assertThat(forbidden.getStackTrace()).isEmpty();
        assertThat(forbidden.category()).isEqualTo(ErrorCategory.FORBIDDEN);
        assertThat(forbidden.errorCode()).isEqualTo(CommonError.FORBIDDEN);

        UnauthenticatedException unauthenticated = new UnauthenticatedException("Unauthenticated");
        assertThat(unauthenticated.getStackTrace()).isEmpty();
        assertThat(unauthenticated.category()).isEqualTo(ErrorCategory.UNAUTHENTICATED);
        assertThat(unauthenticated.errorCode()).isEqualTo(CommonError.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("Internal technical exceptions should capture stack traces for diagnostic forensics")
    void internalExceptions_shouldCaptureStackTrace() {
        InfrastructureException infra = new InfrastructureException("DB error", new RuntimeException("connection timeout"));
        assertThat(infra.getStackTrace()).isNotEmpty();
        assertThat(infra.category()).isEqualTo(ErrorCategory.INTERNAL);
        assertThat(infra.errorCode()).isEqualTo(CommonError.INTERNAL_ERROR);
        assertThat(infra.getCause()).isNotNull();

        ExternalServiceException ext = new ExternalServiceException("HTTP 502", new RuntimeException("bad gateway"));
        assertThat(ext.getStackTrace()).isNotEmpty();
        assertThat(ext.category()).isEqualTo(ErrorCategory.INTERNAL);
        assertThat(ext.errorCode()).isEqualTo(CommonError.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("ResourceNotFoundException should format entity class and id correctly")
    void resourceNotFoundException_withClassAndId() {
        class Customer {}
        ResourceNotFoundException ex1 = new ResourceNotFoundException(Customer.class, "cust-123");
        assertThat(ex1.getMessage()).isEqualTo("Resource 'Customer' with id 'cust-123' not found");

        ResourceNotFoundException ex2 = new ResourceNotFoundException(null, 456);
        assertThat(ex2.getMessage()).isEqualTo("Resource 'Entity' with id '456' not found");
    }

    @Test
    @DisplayName("ValidationException should store immutable violations list and default null to empty")
    void validationException_violationsList() {
        List<FieldViolation> violations = List.of(
                new FieldViolation("name", "name is required"),
                new FieldViolation("age", "must be >= 18")
        );
        ValidationException ex = new ValidationException("Validation failed", violations);

        assertThat(ex.violations()).hasSize(2);
        assertThatThrownBy(() -> ex.violations().add(new FieldViolation("other", "error")))
                .isInstanceOf(UnsupportedOperationException.class);

        ValidationException exNullViolations = new ValidationException("Failed", null);
        assertThat(exNullViolations.violations()).isEmpty();
    }

    @Test
    @DisplayName("ConflictException with custom ErrorCode should retain custom code")
    void conflictException_customErrorCode() {
        ErrorCode customCode = () -> "CUSTOM_DUPLICATE_SLUG";
        ConflictException ex = new ConflictException(customCode, "Slug already exists");

        assertThat(ex.errorCode()).isEqualTo(customCode);
        assertThat(ex.errorCode().code()).isEqualTo("CUSTOM_DUPLICATE_SLUG");
        assertThat(ex.category()).isEqualTo(ErrorCategory.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("Slug already exists");
    }

    @Test
    @DisplayName("ExternalServiceException constructors should properly handle custom codes and causes")
    void externalServiceException_constructors() {
        ErrorCode serviceTimeout = () -> "PAYMENT_GATEWAY_TIMEOUT";
        Throwable rootCause = new RuntimeException("socket read timeout");

        ExternalServiceException ex1 = new ExternalServiceException(serviceTimeout, "Gateway timed out", rootCause);
        assertThat(ex1.errorCode().code()).isEqualTo("PAYMENT_GATEWAY_TIMEOUT");
        assertThat(ex1.getCause()).isSameAs(rootCause);

        ExternalServiceException ex2 = new ExternalServiceException(serviceTimeout, "Gateway timed out");
        assertThat(ex2.errorCode().code()).isEqualTo("PAYMENT_GATEWAY_TIMEOUT");
        assertThat(ex2.getCause()).isNull();
    }

    @Test
    @DisplayName("BaseException should fallback to errorCode name when message is blank or null")
    void baseException_messageFallback() {
        class CustomException extends BaseException {
            CustomException(ErrorCode code, String message) {
                super(code, ErrorCategory.VALIDATION, message);
            }
        }

        CustomException exNull = new CustomException(CommonError.VALIDATION_ERROR, null);
        assertThat(exNull.getMessage()).isEqualTo("VALIDATION_ERROR");

        CustomException exBlank = new CustomException(CommonError.VALIDATION_ERROR, "   ");
        assertThat(exBlank.getMessage()).isEqualTo("VALIDATION_ERROR");
    }
}
