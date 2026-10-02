package com.template.api.shared.application.port.out;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.List;

/**
 * Outbound port for publishing domain events dispatched by aggregate roots.
 * <p>
 * Decouples use cases and domain operations from downstream event dissemination mechanisms,
 * whether published in-memory via Spring's application bus, asynchronously across a message broker,
 * or safely buffered using the Transactional Outbox pattern.
 */
public interface EventPublisherPort {

    void publish(DomainEvent event);
    void publishAll(List<DomainEvent> events);
}