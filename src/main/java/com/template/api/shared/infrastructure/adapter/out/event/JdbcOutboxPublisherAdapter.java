package com.template.api.shared.infrastructure.adapter.out.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.shared.application.port.out.OutboxPublisherPort;
import com.template.api.shared.application.port.out.UuidGeneratorPort;
import com.template.api.shared.domain.event.DomainEvent;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Secondary outbound persistence adapter implementing {@link OutboxPublisherPort} using Spring JDBC and JSONB.
 * <p>
 * Inserts domain events atomically into the {@code outbox_events} table using sequential UUIDv7 identifiers.
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
    private final UuidGeneratorPort uuidGenerator;

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
                UUID tenantId = ExecutionContextHolder.getTenantId();

                String aggregateType = resolveAggregateType(event);

                MapSqlParameterSource params = new MapSqlParameterSource()
                        .addValue("eventId", uuidGenerator.generateId())
                        .addValue("tenantId", tenantId)
                        .addValue("aggType", aggregateType)
                        .addValue("aggId", event.aggregateId())
                        .addValue("eventType", event.eventType())
                        .addValue("payload", payload)
                        .addValue("corrId", correlationId);

                jdbcTemplate.update(OUTBOX_INSERT_SQL, params);
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
