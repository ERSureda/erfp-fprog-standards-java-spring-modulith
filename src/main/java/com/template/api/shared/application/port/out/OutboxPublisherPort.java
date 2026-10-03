package com.template.api.shared.application.port.out;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.List;

/**
 * Outbound port for recording domain events into the transactional outbox buffer.
 * Conforms to TRX-03 and SED-05.
 */
public interface OutboxPublisherPort {

    void publish(DomainEvent event);

    void publishAll(List<DomainEvent> events);
}
