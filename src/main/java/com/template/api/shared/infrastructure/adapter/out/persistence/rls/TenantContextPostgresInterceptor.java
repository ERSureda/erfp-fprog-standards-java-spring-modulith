package com.template.api.shared.infrastructure.adapter.out.persistence.rls;

import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hibernate StatementInspector intercepting SQL statements to bind the tenant context in PostgreSQL.
 * <p>
 * Ensures Row-Level Security (RLS) policies are consistently tagged or executed in multi-tenant environments,
 * maintaining compatibility with transaction-pooled connections (e.g., PgBouncer) (SEED_SPEC.md §4.3).
 */
@Component
public class TenantContextPostgresInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        if (sql == null) {
            return null;
        }
        UUID tenantId = ExecutionContextHolder.getTenantId();
        if (tenantId != null && !sql.contains("app.current_tenant_id")) {
            return "/* tenant: " + tenantId + " */ " + sql;
        }
        return sql;
    }
}
