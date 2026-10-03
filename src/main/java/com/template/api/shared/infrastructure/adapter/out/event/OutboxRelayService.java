package com.template.api.shared.infrastructure.adapter.out.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.shared.application.port.out.EventPublisherPort;
import com.template.api.shared.domain.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled background relay service dispatching unpublished transactional outbox events.
 * <p>
 * Implements non-contentious pessimistic locking using {@code FOR UPDATE SKIP LOCKED} to prevent
 * worker contention in horizontally scaled deployments (SED-05, TRX-03). Applies exponential
 * backoff retry logic, moves poisoned events to DEAD_LETTER after 5 attempts, and runs periodic
 * purging of historical DELIVERED events (TRX-06).
 * Programmatically manages transactions via {@link TransactionOperations} to preserve strict
 * confinement of the {@code @Transactional} annotation to the application use-case boundary (TRX-01).
 */
@Service
@ConditionalOnBean(NamedParameterJdbcTemplate.class)
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final Map<String, Class<?>> EVENT_CLASS_CACHE = new ConcurrentHashMap<>(16);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EventPublisherPort eventPublisherPort;
    private final ObjectMapper objectMapper;
    private final TransactionOperations transactionOperations;

    @Autowired
    public OutboxRelayService(
            NamedParameterJdbcTemplate jdbcTemplate,
            EventPublisherPort eventPublisherPort,
            ObjectMapper objectMapper,
            @Autowired(required = false) TransactionOperations transactionOperations
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisherPort = eventPublisherPort;
        this.objectMapper = objectMapper;
        this.transactionOperations = transactionOperations != null
                ? transactionOperations
                : TransactionOperations.withoutTransaction();
    }

    public OutboxRelayService(
            NamedParameterJdbcTemplate jdbcTemplate,
            EventPublisherPort eventPublisherPort,
            ObjectMapper objectMapper
    ) {
        this(jdbcTemplate, eventPublisherPort, objectMapper, TransactionOperations.withoutTransaction());
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void processPendingEvents() {
        transactionOperations.executeWithoutResult(_ -> {
            String selectSql = """
                SELECT event_id, event_type, payload, retry_count
                FROM outbox_events
                WHERE status = 'PENDING'
                   OR (status = 'FAILED' AND next_retry_at <= NOW())
                ORDER BY created_at ASC
                LIMIT 50
                FOR UPDATE SKIP LOCKED
            """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql, Map.of());
            if (rows.isEmpty()) {
                return;
            }

            for (Map<String, Object> row : rows) {
                UUID eventId = (UUID) row.get("event_id");
                String eventType = (String) row.get("event_type");
                String payloadJson = (String) row.get("payload");
                int retryCount = ((Number) row.get("retry_count")).intValue();

                try {
                    jdbcTemplate.update(
                        "UPDATE outbox_events SET status = 'PROCESSING', locked_at = NOW() WHERE event_id = :id",
                        Map.of("id", eventId)
                    );

                    Class<?> eventClass = resolveEventClass(eventType);
                    DomainEvent domainEvent = (DomainEvent) objectMapper.readValue(payloadJson, eventClass);
                    eventPublisherPort.publish(domainEvent);

                    jdbcTemplate.update(
                        "UPDATE outbox_events SET status = 'DELIVERED', locked_at = NULL, next_retry_at = NULL WHERE event_id = :id",
                        Map.of("id", eventId)
                    );
                } catch (Exception ex) {
                    log.error("Failed to relay outbox event [id={}]", eventId, ex);
                    int nextRetry = retryCount + 1;
                    long delaySeconds = (long) Math.pow(2, nextRetry);

                    jdbcTemplate.update("""
                        UPDATE outbox_events
                        SET status = CASE WHEN :nextRetry >= 5 THEN 'DEAD_LETTER' ELSE 'FAILED' END,
                            retry_count = :nextRetry,
                            last_error = :error,
                            locked_at = NULL,
                            next_retry_at = NOW() + INTERVAL '1 second' * :delay
                        WHERE event_id = :id
                    """, Map.of(
                        "id", eventId,
                        "nextRetry", nextRetry,
                        "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                        "delay", delaySeconds
                    ));
                }
            }
        });
    }

    @Scheduled(cron = "${outbox.purge-cron:0 0 3 * * ?}")
    public void purgeDeliveredEvents() {
        transactionOperations.executeWithoutResult(_ -> {
            int retentionDays = 7;
            String deleteSql = """
                DELETE FROM outbox_events
                WHERE status = 'DELIVERED'
                  AND created_at < NOW() - INTERVAL '1 day' * :days
            """;
            int purgedRows = jdbcTemplate.update(deleteSql, Map.of("days", retentionDays));
            if (purgedRows > 0) {
                log.info("Purged {} delivered outbox events older than {} days", purgedRows, retentionDays);
            }
        });
    }

    private static Class<?> resolveEventClass(String eventType) throws ClassNotFoundException {
        Class<?> cached = EVENT_CLASS_CACHE.get(eventType);
        if (cached != null) {
            return cached;
        }
        Class<?> loaded = Class.forName(eventType);
        EVENT_CLASS_CACHE.put(eventType, loaded);
        return loaded;
    }
}
