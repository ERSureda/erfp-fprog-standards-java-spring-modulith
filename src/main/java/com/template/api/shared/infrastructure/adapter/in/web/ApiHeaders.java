package com.template.api.shared.infrastructure.adapter.in.web;

/**
 * Standard HTTP header definitions propagated by reverse proxies or API gateways.
 */
public final class ApiHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String ROLES = "X-Roles";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private ApiHeaders() {}
}