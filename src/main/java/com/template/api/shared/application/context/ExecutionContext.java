package com.template.api.shared.application.context;

import com.template.api.shared.domain.exception.ForbiddenException;
import com.template.api.shared.domain.exception.UnauthenticatedException;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable execution context carrying identity, multitenancy, authorization, and tracing metadata.
 * <p>
 * Decoupled from transport protocols (HTTP headers, tokens, message queues), this record provides
 * application use cases with an authenticated snapshot of the calling actor.
 * Conforms to SED-03.
 *
 * @param userType      classification of the calling actor
 * @param tenantId      target tenant identifier, or null for global admins or tenantless requests
 * @param userId        authenticated user identifier, or null for anonymous executions
 * @param roles         immutable set of functional roles granted within the actor scope
 * @param correlationId end-to-end distributed tracing correlation identifier
 */
public record ExecutionContext(
        UserType userType,
        UUID tenantId,
        UUID userId,
        Set<String> roles,
        String correlationId
) {

    private static final ExecutionContext ANONYMOUS = new ExecutionContext(
            UserType.ANONYMOUS,
            null,
            null,
            Set.of(),
            null
    );

    public ExecutionContext {
        userType = (userType != null) ? userType : UserType.ANONYMOUS;
        roles = (roles == null || roles.isEmpty()) ? Set.of() : Set.copyOf(roles);
        correlationId = (correlationId != null && !correlationId.isBlank()) ? correlationId.trim() : null;
    }

    public ExecutionContext(UUID tenantId, UUID userId, Set<String> roles, String correlationId) {
        this(resolveUserType(tenantId, userId, roles), tenantId, userId, roles, correlationId);
    }

    public ExecutionContext(UUID tenantId, UUID userId, Set<String> roles) {
        this(tenantId, userId, roles, null);
    }

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

    public boolean hasCorrelationId() {
        return correlationId != null;
    }

    public boolean hasRole(String role) {
        return role != null && roles.contains(role);
    }

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

    public UUID requireUserId() {
        if (!isAuthenticated()) {
            throw new UnauthenticatedException("Authentication required to execute this operation");
        }
        return userId;
    }

    public UUID requireTenantId() {
        if (tenantId == null) {
            throw new ForbiddenException("Tenant context is required for this operation");
        }
        return tenantId;
    }

    public boolean hasTenantRole(String role) {
        return hasTenant() && hasRole(role);
    }

    public void requireTenantRole(String role) {
        requireTenantId();
        if (!hasRole(role)) {
            throw new ForbiddenException("Missing required tenant role: " + role);
        }
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
