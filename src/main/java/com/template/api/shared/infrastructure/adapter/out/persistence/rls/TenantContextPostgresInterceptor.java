package com.template.api.shared.infrastructure.adapter.out.persistence.rls;

import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hibernate StatementInspector tagging SQL statements with the active tenant context for PostgreSQL.
 * <p>
 * Ensures Row-Level Security (RLS) policies and audit logs receive tenant metadata across connection pools.
 * Conforms to SED-03.
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
