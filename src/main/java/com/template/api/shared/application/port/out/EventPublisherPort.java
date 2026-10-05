package com.template.api.shared.application.port.out;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.List;

/**
 * Outbound port for publishing domain events dispatched by aggregate roots.
 * <p>
 * Decouples use cases and domain operations from downstream event dissemination mechanisms.
 * Conforms to DOM-01 and TRX-03.
 */
public interface EventPublisherPort {

    void publish(DomainEvent event);

    void publishAll(List<DomainEvent> events);
}
