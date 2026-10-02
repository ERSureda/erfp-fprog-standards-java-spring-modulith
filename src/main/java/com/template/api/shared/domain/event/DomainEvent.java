package com.template.api.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base contract representing an immutable domain event in Domain-Driven Design (DDD).
 * <p>
 * Domain events represent past occurrences and state transitions within an aggregate root.
 * Implementations should be declared as immutable {@code record} types matching these accessors.
 * Standardizes event identity, source aggregate, occurrence timestamp, and semantic event type
 * for serialization, outbox pattern persistence, and asynchronous routing.
 */
public interface DomainEvent {

    /**
     * Unique identifier of the event instance.
     */
    UUID eventId();

    /**
     * Identifier of the aggregate root that produced this event.
     */
    String aggregateId();

    /**
     * Exact point in time in UTC when the event occurred.
     */
    Instant occurredAt();

    /**
     * Explicit semantic type identifier of the event (e.g., 'orders.created.v1').
     */
    String eventType();
}