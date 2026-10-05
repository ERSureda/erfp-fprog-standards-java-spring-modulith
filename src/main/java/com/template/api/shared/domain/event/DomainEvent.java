package com.template.api.shared.domain.event;

import java.time.Instant;

/**
 * Common contract for immutable domain events in Domain-Driven Design (DDD).
 * <p>
 * Represents state transitions and business facts that have occurred within an aggregate root.
 * Implementations are typically declared as immutable record types.
 * Conforms to DOM-01 and TRX-03.
 */
public interface DomainEvent {

    String aggregateId();

    default Instant occurredAt() {
        return Instant.now();
    }

    default String eventType() {
        return getClass().getName();
    }
}
