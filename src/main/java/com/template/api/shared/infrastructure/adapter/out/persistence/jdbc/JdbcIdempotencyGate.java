package com.template.api.shared.infrastructure.adapter.out.persistence.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Shared JDBC adapter for consumer idempotency checking against {@code processed_events}.
 * Conforms to OUT-05 and TRX-05.
 */
@Component
@RequiredArgsConstructor
public class JdbcIdempotencyGate {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Attempts to acquire an idempotency lock for the given event ID.
     * Returns {@code true} if newly acquired, or {@code false} if already processed.
     */
    public boolean tryAcquire(UUID eventId, String consumerName) {
        String sql = """
            INSERT INTO processed_events (event_id, consumer_name, processed_at)
            VALUES (:eventId, :consumerName, clock_timestamp())
            ON CONFLICT (event_id) DO NOTHING
        """;
        int rows = jdbcTemplate.update(sql, Map.of(
                "eventId", eventId,
                "consumerName", consumerName
        ));
        return rows > 0;
    }

    /**
     * Releases an idempotency reservation in case of downstream execution failure,
     * allowing subsequent message retries to be re-evaluated.
     *
     * @param eventId identifier of the event to release
     * @return {@code true} if an existing record was removed, {@code false} otherwise
     */
    public boolean release(UUID eventId) {
        String sql = "DELETE FROM processed_events WHERE event_id = :eventId";
        int rows = jdbcTemplate.update(sql, Map.of("eventId", eventId));
        return rows > 0;
    }

    /**
     * Checks if the event has already been recorded in processed_events.
     */
    public boolean isProcessed(UUID eventId) {
        String sql = "SELECT COUNT(*) FROM processed_events WHERE event_id = :eventId";
        Integer count = jdbcTemplate.queryForObject(sql, Map.of("eventId", eventId), Integer.class);
        return count != null && count > 0;
    }
}
