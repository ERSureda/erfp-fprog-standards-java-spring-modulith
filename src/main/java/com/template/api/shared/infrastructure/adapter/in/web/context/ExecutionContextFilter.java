package com.template.api.shared.infrastructure.adapter.in.web.context;

import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.infrastructure.adapter.in.web.ApiHeaders;
import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Perimeter HTTP filter extracting execution context metadata from gateway headers.
 * <p>
 * Reconstructs the immutable {@link ExecutionContext} (tenant, user, and roles) injected by
 * reverse proxies or API gateways, binding it to thread-local storage and logging MDC.
 * Guarantees non-destructive scoped cleanup in the {@code finally} block to prevent context leaks.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ExecutionContextFilter extends OncePerRequestFilter {

    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String rawTenantId = request.getHeader(ApiHeaders.TENANT_ID);
        String rawUserId = request.getHeader(ApiHeaders.USER_ID);

        UUID tenantId = parseUuid(rawTenantId);
        UUID userId = parseUuid(rawUserId);
        Set<String> roles = parseRoles(request.getHeader(ApiHeaders.ROLES));

        ExecutionContextHolder.set(new ExecutionContext(tenantId, userId, roles));

        if (tenantId != null) {
            MDC.put(MDC_TENANT_ID, rawTenantId.trim());
        }
        if (userId != null) {
            MDC.put(MDC_USER_ID, rawUserId.trim());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            ExecutionContextHolder.clear();
            if (tenantId != null) {
                MDC.remove(MDC_TENANT_ID);
            }
            if (userId != null) {
                MDC.remove(MDC_USER_ID);
            }
        }
    }

    private static UUID parseUuid(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(header.trim());
        } catch (IllegalArgumentException _) {
            return null;
        }
    }

    private static Set<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return Set.of();
        }
        if (!header.contains(",")) {
            String role = header.trim();
            return role.isEmpty() ? Set.of() : Set.of(role);
        }

        Set<String> roles = new HashSet<>(4);
        for (String part : header.split(",")) {
            String role = part.trim();
            if (!role.isEmpty()) {
                roles.add(role);
            }
        }
        return Set.copyOf(roles);
    }
}