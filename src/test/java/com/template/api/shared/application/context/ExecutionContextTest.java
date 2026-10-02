package com.template.api.shared.application.context;

import com.template.api.shared.domain.exception.ForbiddenException;
import com.template.api.shared.domain.exception.UnauthenticatedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExecutionContext Unit Tests")
class ExecutionContextTest {

    @Test
    @DisplayName("Should create context with full attributes")
    void fullAttributes_shouldStoreValues() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");

        ExecutionContext context = new ExecutionContext(tenantId, userId, roles);

        assertThat(context.tenantId()).isEqualTo(tenantId);
        assertThat(context.userId()).isEqualTo(userId);
        assertThat(context.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(context.isAuthenticated()).isTrue();
        assertThat(context.hasTenant()).isTrue();
        assertThat(context.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(context.hasRole("ROLE_GUEST")).isFalse();
        assertThat(context.hasRole(null)).isFalse();
    }

    @Test
    @DisplayName("Should resolve UserType.SAAS_ADMIN when role contains SAAS_ADMIN or ROLE_SAAS_ADMIN")
    void resolveUserType_saasAdmin() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ExecutionContext ctx1 = new ExecutionContext(tenantId, userId, Set.of("SAAS_ADMIN"));
        assertThat(ctx1.userType()).isEqualTo(UserType.SAAS_ADMIN);
        assertThat(ctx1.isSaasAdmin()).isTrue();
        assertThat(ctx1.isTenantUser()).isFalse();
        assertThat(ctx1.isClient()).isFalse();

        ExecutionContext ctx2 = new ExecutionContext(null, userId, Set.of("ROLE_SAAS_ADMIN"));
        assertThat(ctx2.userType()).isEqualTo(UserType.SAAS_ADMIN);
        assertThat(ctx2.isSaasAdmin()).isTrue();
    }

    @Test
    @DisplayName("Should resolve UserType.TENANT_USER when user has tenantId and is not SAAS_ADMIN")
    void resolveUserType_tenantUser() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ExecutionContext ctx = new ExecutionContext(tenantId, userId, Set.of("USER"));
        assertThat(ctx.userType()).isEqualTo(UserType.TENANT_USER);
        assertThat(ctx.isTenantUser()).isTrue();
        assertThat(ctx.isSaasAdmin()).isFalse();
        assertThat(ctx.isClient()).isFalse();
    }

    @Test
    @DisplayName("Should resolve UserType.CLIENT when user has no tenantId and is not SAAS_ADMIN")
    void resolveUserType_client() {
        UUID userId = UUID.randomUUID();

        ExecutionContext ctx = new ExecutionContext(null, userId, Set.of("CLIENT_ROLE"));
        assertThat(ctx.userType()).isEqualTo(UserType.CLIENT);
        assertThat(ctx.isClient()).isTrue();
        assertThat(ctx.isTenantUser()).isFalse();
        assertThat(ctx.isSaasAdmin()).isFalse();
    }

    @Test
    @DisplayName("Should resolve UserType.ANONYMOUS when userId is null")
    void resolveUserType_anonymous() {
        UUID tenantId = UUID.randomUUID();

        ExecutionContext ctx = new ExecutionContext(tenantId, null, Set.of("SOME_ROLE"));
        assertThat(ctx.userType()).isEqualTo(UserType.ANONYMOUS);
        assertThat(ctx.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Should handle null attributes gracefully in canonical constructor")
    void nullAttributes_shouldDefaultSafely() {
        ExecutionContext ctx = new ExecutionContext(null, null, null, null);
        assertThat(ctx.userType()).isEqualTo(UserType.ANONYMOUS);
        assertThat(ctx.roles()).isEmpty();
        assertThat(ctx.isAuthenticated()).isFalse();
        assertThat(ctx.hasTenant()).isFalse();
    }

    @Test
    @DisplayName("Should return singleton for anonymous context")
    void anonymous_factory() {
        ExecutionContext anonymous = ExecutionContext.anonymous();
        assertThat(anonymous.isAuthenticated()).isFalse();
        assertThat(anonymous.hasTenant()).isFalse();
        assertThat(anonymous.roles()).isEmpty();
        assertThat(anonymous.userType()).isEqualTo(UserType.ANONYMOUS);
        assertThat(ExecutionContext.anonymous()).isSameAs(anonymous);
    }

    @Test
    @DisplayName("Should ensure roles set is unmodifiable")
    void roles_shouldBeUnmodifiable() {
        Set<String> mutableRoles = new HashSet<>();
        mutableRoles.add("ROLE_USER");

        ExecutionContext context = new ExecutionContext(null, null, mutableRoles);
        mutableRoles.add("ROLE_ADMIN");

        assertThat(context.roles()).containsExactly("ROLE_USER");
        assertThatThrownBy(() -> context.roles().add("ROLE_OTHER"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should correctly evaluate hasAnyRole")
    void hasAnyRole_shouldCheckMultipleRoles() {
        ExecutionContext context = new ExecutionContext(null, UUID.randomUUID(), Set.of("ROLE_USER", "ROLE_EDITOR"));

        assertThat(context.hasAnyRole("ROLE_ADMIN", "ROLE_EDITOR")).isTrue();
        assertThat(context.hasAnyRole("ROLE_ADMIN", "ROLE_SUPERUSER")).isFalse();
        assertThat(context.hasAnyRole((String[]) null)).isFalse();
        assertThat(context.hasAnyRole()).isFalse();
        assertThat(context.hasAnyRole((String) null)).isFalse();

        ExecutionContext emptyRoles = new ExecutionContext(null, null, Set.of());
        assertThat(emptyRoles.hasAnyRole("ROLE_USER")).isFalse();
    }

    @Test
    @DisplayName("requireUserId should return userId when authenticated, throw UnauthenticatedException when anonymous")
    void requireUserId_shouldEnforceAuthentication() {
        UUID userId = UUID.randomUUID();
        ExecutionContext authenticated = new ExecutionContext(null, userId, Set.of());
        assertThat(authenticated.requireUserId()).isEqualTo(userId);

        ExecutionContext anonymous = ExecutionContext.anonymous();
        assertThatThrownBy(anonymous::requireUserId)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("Authentication required to execute this operation");
    }

    @Test
    @DisplayName("requireTenantId should return tenantId when present, throw ForbiddenException when absent")
    void requireTenantId_shouldEnforceTenantContext() {
        UUID tenantId = UUID.randomUUID();
        ExecutionContext withTenant = new ExecutionContext(tenantId, UUID.randomUUID(), Set.of());
        assertThat(withTenant.requireTenantId()).isEqualTo(tenantId);

        ExecutionContext withoutTenant = new ExecutionContext(null, UUID.randomUUID(), Set.of());
        assertThatThrownBy(withoutTenant::requireTenantId)
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Tenant context is required for this operation");
    }
}