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

/**
 * Test suite for {@link ExecutionContext}.
 * <p>
 * Verifies caller classification, role verification, tenant guards, and immutable state defense.
 */
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
        ExecutionContext ctx2 = new ExecutionContext(null, userId, Set.of("ROLE_SAAS_ADMIN"));

        assertThat(ctx1.userType()).isEqualTo(UserType.SAAS_ADMIN);
        assertThat(ctx1.isSaasAdmin()).isTrue();
        assertThat(ctx1.isTenantUser()).isFalse();
        assertThat(ctx1.isClient()).isFalse();

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

        ExecutionContext ctx = new ExecutionContext(null, userId, Set.of("USER"));

        assertThat(ctx.userType()).isEqualTo(UserType.CLIENT);
        assertThat(ctx.isClient()).isTrue();
        assertThat(ctx.isTenantUser()).isFalse();
        assertThat(ctx.isSaasAdmin()).isFalse();
    }

    @Test
    @DisplayName("Should resolve UserType.ANONYMOUS when userId is null")
    void resolveUserType_anonymous() {
        ExecutionContext ctx = new ExecutionContext(null, null, Set.of("GUEST"));

        assertThat(ctx.userType()).isEqualTo(UserType.ANONYMOUS);
        assertThat(ctx.isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("Should provide cached anonymous singleton")
    void anonymous_singleton() {
        ExecutionContext anon = ExecutionContext.anonymous();

        assertThat(anon.userType()).isEqualTo(UserType.ANONYMOUS);
        assertThat(anon.userId()).isNull();
        assertThat(anon.tenantId()).isNull();
        assertThat(anon.roles()).isEmpty();
        assertThat(anon.correlationId()).isNull();
        assertThat(anon.isAuthenticated()).isFalse();
        assertThat(anon.hasTenant()).isFalse();
        assertThat(anon.hasCorrelationId()).isFalse();
    }

    @Test
    @DisplayName("Should support 4-argument constructor with correlationId")
    void constructorWithCorrelationId_shouldStoreTrimmedValue() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ExecutionContext ctx = new ExecutionContext(tenantId, userId, Set.of("USER"), "  corr-123  ");

        assertThat(ctx.correlationId()).isEqualTo("corr-123");
        assertThat(ctx.hasCorrelationId()).isTrue();
    }

    @Test
    @DisplayName("Blank correlationId should be normalized to null")
    void blankCorrelationId_shouldBeNull() {
        ExecutionContext ctx = new ExecutionContext(null, null, null, "   ");

        assertThat(ctx.correlationId()).isNull();
        assertThat(ctx.hasCorrelationId()).isFalse();
    }

    @Test
    @DisplayName("hasAnyRole should check against varargs without collection allocations")
    void hasAnyRole_behavior() {
        ExecutionContext ctx = new ExecutionContext(null, UUID.randomUUID(), Set.of("OPERATOR", "VIEWER"));

        assertThat(ctx.hasAnyRole("ADMIN", "OPERATOR")).isTrue();
        assertThat(ctx.hasAnyRole("ADMIN", "SUPERUSER")).isFalse();
        assertThat(ctx.hasAnyRole()).isFalse();
        assertThat(ctx.hasAnyRole((String[]) null)).isFalse();
        assertThat(ctx.hasAnyRole((String) null)).isFalse();

        ExecutionContext emptyRolesCtx = new ExecutionContext(null, UUID.randomUUID(), Set.of());
        assertThat(emptyRolesCtx.hasAnyRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("requireUserId should return userId or throw UnauthenticatedException")
    void requireUserId_behavior() {
        UUID userId = UUID.randomUUID();
        ExecutionContext authed = new ExecutionContext(null, userId, Set.of());
        ExecutionContext anon = ExecutionContext.anonymous();

        assertThat(authed.requireUserId()).isEqualTo(userId);
        assertThatThrownBy(anon::requireUserId)
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessageContaining("Authentication required");
    }

    @Test
    @DisplayName("requireTenantId should return tenantId or throw ForbiddenException")
    void requireTenantId_behavior() {
        UUID tenantId = UUID.randomUUID();
        ExecutionContext withTenant = new ExecutionContext(tenantId, UUID.randomUUID(), Set.of());
        ExecutionContext withoutTenant = new ExecutionContext(null, UUID.randomUUID(), Set.of());

        assertThat(withTenant.requireTenantId()).isEqualTo(tenantId);
        assertThatThrownBy(withoutTenant::requireTenantId)
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("hasTenantRole should verify both tenant existence and role presence")
    void hasTenantRole_behavior() {
        UUID tenantId = UUID.randomUUID();
        ExecutionContext ctx = new ExecutionContext(tenantId, UUID.randomUUID(), Set.of("TENANT_ADMIN"));
        ExecutionContext noTenantCtx = new ExecutionContext(null, UUID.randomUUID(), Set.of("TENANT_ADMIN"));

        assertThat(ctx.hasTenantRole("TENANT_ADMIN")).isTrue();
        assertThat(ctx.hasTenantRole("TENANT_USER")).isFalse();
        assertThat(noTenantCtx.hasTenantRole("TENANT_ADMIN")).isFalse();
    }

    @Test
    @DisplayName("requireTenantRole should succeed or throw ForbiddenException")
    void requireTenantRole_behavior() {
        UUID tenantId = UUID.randomUUID();
        ExecutionContext validCtx = new ExecutionContext(tenantId, UUID.randomUUID(), Set.of("TENANT_ADMIN"));
        ExecutionContext missingRoleCtx = new ExecutionContext(tenantId, UUID.randomUUID(), Set.of("TENANT_USER"));
        ExecutionContext missingTenantCtx = new ExecutionContext(null, UUID.randomUUID(), Set.of("TENANT_ADMIN"));

        validCtx.requireTenantRole("TENANT_ADMIN");

        assertThatThrownBy(() -> missingRoleCtx.requireTenantRole("TENANT_ADMIN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Missing required tenant role: TENANT_ADMIN");

        assertThatThrownBy(() -> missingTenantCtx.requireTenantRole("TENANT_ADMIN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Tenant context is required");
    }

    @Test
    @DisplayName("Roles collection should be unmodifiable and defensively copied")
    void rolesImmutability_shouldDefendAgainstMutation() {
        Set<String> mutableRoles = new HashSet<>();
        mutableRoles.add("ROLE_A");

        ExecutionContext ctx = new ExecutionContext(null, UUID.randomUUID(), mutableRoles);
        mutableRoles.add("ROLE_B");

        assertThat(ctx.roles()).containsExactly("ROLE_A");
        assertThatThrownBy(() -> ctx.roles().add("ROLE_C"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
