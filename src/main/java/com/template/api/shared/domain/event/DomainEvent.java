package com.template.api.shared.domain.event;

import java.time.Instant;

/**
 * Base contract representing an immutable domain event in Domain-Driven Design (DDD).
 * <p>
 * Domain events represent past business occurrences and state transitions within an aggregate root.
 * Implementations should be declared as immutable {@code record} types containing pure business payloads.
 * Technical transportation metadata (like sequential event ID generation) is handled at infrastructure boundaries.
 * Conforms to DOM-01 and TRX-03.
 */
public interface DomainEvent {

    /**
     * Identifier of the aggregate root that produced this event.
     * Serves as partition/sharding key in event brokers and persistence stores.
     */
    String aggregateId();

    /**
     * Exact point in time in UTC when the event occurred.
     * Defaults to the current UTC instant.
     */
    default Instant occurredAt() {
        return Instant.now();
    }

    /**
     * Explicit semantic type identifier of the event.
     * Defaults to the fully qualified class name for automated deserialization.
     */
    default String eventType() {
        return getClass().getName();
    }
}