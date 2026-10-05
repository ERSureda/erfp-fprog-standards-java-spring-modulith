package com.template.api.shared.infrastructure.adapter.in.web;

/**
 * Standard HTTP header constant definitions propagated by API gateways and reverse proxies.
 * <p>
 * Defines header keys consumed by perimeter filters to establish request context.
 * Conforms to SED-03.
 */
public final class ApiHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String ROLES = "X-Roles";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private ApiHeaders() {}
}
