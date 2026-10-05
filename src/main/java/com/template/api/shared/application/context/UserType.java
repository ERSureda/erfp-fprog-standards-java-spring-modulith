package com.template.api.shared.application.context;

/**
 * Classification of execution actors operating within the platform.
 * <p>
 * Distinguishes security profiles between platform administrators, tenant members, clients, and anonymous actors.
 * Conforms to SED-03.
 */
public enum UserType {
    ANONYMOUS,
    CLIENT,
    TENANT_USER,
    SAAS_ADMIN
}
