package com.template.api.shared.domain.error;

/**
 * Semantic classification of errors for HTTP status mapping and JVM diagnostic control.
 * <p>
 * Optimizes JVM resource usage: business flow errors bypass costly stack trace generation,
 * while unexpected internal failures capture full diagnostics.
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