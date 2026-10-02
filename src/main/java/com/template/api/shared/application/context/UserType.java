package com.template.api.shared.application.context;

/**
 * Classification of the execution actor operating within the platform.
 * <p>
 * Distinguishes between global platform administrators, tenant-scoped internal users,
 * and end clients without forcing role-string parsing heuristics.
 */
public enum UserType {
    ANONYMOUS,
    CLIENT,
    TENANT_USER,
    SAAS_ADMIN
}