package com.template.api.shared.domain.error;

/**
 * Semantic classification of domain and system errors for HTTP mapping and diagnostics.
 * <p>
 * Determines whether the exception captures full JVM stack traces or bypasses them for zero overhead.
 * Conforms to DOM-01 and ADR-003.
 */
public enum ErrorCategory {

    VALIDATION(false),
    NOT_FOUND(false),
    CONFLICT(false),
    UNAUTHENTICATED(false),
    FORBIDDEN(false),
    INTERNAL(true);

    private final boolean capturesDiagnostics;

    ErrorCategory(boolean capturesDiagnostics) {
        this.capturesDiagnostics = capturesDiagnostics;
    }

    public boolean capturesDiagnostics() {
        return capturesDiagnostics;
    }
}
