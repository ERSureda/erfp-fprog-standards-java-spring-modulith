package com.template.api.shared.infrastructure.adapter.out.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.shared.application.port.out.OutboxPublisherPort;
import com.template.api.shared.domain.event.DomainEvent;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Secondary adapter implementing OutboxPublisherPort using NamedParameterJdbcTemplate and PostgreSQL JSONB.
 * Conforms to TRX-03, SED-05, and OUT-05.
 */
@Component
@RequiredArgsConstructor
public class JdbcOutboxPublisherAdapter implements OutboxPublisherPort {

    private static final String OUTBOX_INSERT_SQL = """
        INSERT INTO outbox_events (
            event_id, tenant_id, aggregate_type, aggregate_id,
            event_type, payload, correlation_id, status
        ) VALUES (
            :eventId, :tenantId, :aggType, :aggId,
            :eventType, CAST(:payload AS jsonb), :corrId, 'PENDING'
        )
    """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }
        publishAll(List.of(event));
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        for (DomainEvent event : events) {
            if (event == null) {
                continue;
            }
            try {
                String payload = objectMapper.writeValueAsString(event);
                String correlationId = ExecutionContextHolder.get() != null
                        ? ExecutionContextHolder.get().correlationId()
                        : null;
                String aggregateType = resolveAggregateType(event);

                MapSqlParameterSource paramSource = new MapSqlParameterSource();
                paramSource.addValue("eventId", event.eventId());
                paramSource.addValue("tenantId", ExecutionContextHolder.getTenantId());
                paramSource.addValue("aggType", aggregateType);
                paramSource.addValue("aggId", event.aggregateId());
                paramSource.addValue("eventType", event.getClass().getName());
                paramSource.addValue("payload", payload);
                paramSource.addValue("corrId", correlationId);

                jdbcTemplate.update(OUTBOX_INSERT_SQL, paramSource);
            } catch (Exception ex) {
                throw new InfrastructureException("Failed to persist domain event to outbox", ex);
            }
        }
    }

    private String resolveAggregateType(DomainEvent event) {
        String simpleName = event.getClass().getSimpleName();
        if (simpleName.endsWith("Event")) {
            simpleName = simpleName.substring(0, simpleName.length() - 5);
        }
        return simpleName.isEmpty() ? "Unknown" : simpleName;
    }
}
