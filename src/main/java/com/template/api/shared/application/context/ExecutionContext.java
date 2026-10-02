package com.template.api.shared.application.context;


import com.template.api.shared.domain.exception.ForbiddenException;
import com.template.api.shared.domain.exception.UnauthenticatedException;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable execution context carrying identity, multitenancy, and authorization metadata.
 * <p>
 * Decoupled from transport protocols (HTTP headers, tokens, message queues), this record provides
 * application use cases with an authenticated snapshot of the calling actor. Enforces security and
 * tenant boundaries via fail-fast guard methods with zero intermediate heap allocations.
 *
 * @param userType classification of the calling actor
 * @param tenantId target tenant identifier, or {@code null} for global admins or tenantless requests
 * @param userId   authenticated user identifier, or {@code null} for anonymous executions
 * @param roles    immutable set of functional roles granted within the actor's scope
 */
public record ExecutionContext(
        UserType userType,
        UUID tenantId,
        UUID userId,
        Set<String> roles
) {

    private static final ExecutionContext ANONYMOUS = new ExecutionContext(
            UserType.ANONYMOUS,
            null,
            null,
            Set.of()
    );

    public ExecutionContext {
        userType = (userType != null) ? userType : UserType.ANONYMOUS;
        roles = (roles == null || roles.isEmpty()) ? Set.of() : Set.copyOf(roles);
    }

    /**
     * Convenience constructor inferring {@link UserType} automatically from identity and roles.
     */
    public ExecutionContext(UUID tenantId, UUID userId, Set<String> roles) {
        this(resolveUserType(tenantId, userId, roles), tenantId, userId, roles);
    }

    /**
     * Returns the cached singleton context for unauthenticated operations.
     */
    public static ExecutionContext anonymous() {
        return ANONYMOUS;
    }

    public boolean isAuthenticated() {
        return userId != null && userType != UserType.ANONYMOUS;
    }

    public boolean isSaasAdmin() {
        return userType == UserType.SAAS_ADMIN;
    }

    public boolean isTenantUser() {
        return userType == UserType.TENANT_USER;
    }

    public boolean isClient() {
        return userType == UserType.CLIENT;
    }

    public boolean hasTenant() {
        return tenantId != null;
    }

    public boolean hasRole(String role) {
        return role != null && roles.contains(role);
    }

    /**
     * Checks if the context contains at least one of the specified roles without allocating collections.
     */
    public boolean hasAnyRole(String... checkRoles) {
        if (checkRoles == null || checkRoles.length == 0 || roles.isEmpty()) {
            return false;
        }
        for (String role : checkRoles) {
            if (role != null && roles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asserts that the executing actor is authenticated.
     *
     * @return verified non-null user identifier
     * @throws UnauthenticatedException if the context is anonymous
     */
    public UUID requireUserId() {
        if (!isAuthenticated()) {
            throw new UnauthenticatedException("Authentication required to execute this operation");
        }
        return userId;
    }

    /**
     * Asserts that the execution is bound to a tenant.
     *
     * @return verified non-null tenant identifier
     * @throws ForbiddenException if no tenant is associated
     */
    public UUID requireTenantId() {
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context is required for this operation");
        }
        return tenantId;
    }

    private static UserType resolveUserType(UUID tenantId, UUID userId, Set<String> roles) {
        if (userId == null) {
            return UserType.ANONYMOUS;
        }
        if (roles != null && (roles.contains("SAAS_ADMIN") || roles.contains("ROLE_SAAS_ADMIN"))) {
            return UserType.SAAS_ADMIN;
        }
        return (tenantId != null) ? UserType.TENANT_USER : UserType.CLIENT;
    }
}