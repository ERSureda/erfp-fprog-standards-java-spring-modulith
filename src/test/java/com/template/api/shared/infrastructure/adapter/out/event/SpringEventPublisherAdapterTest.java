package com.template.api.shared.infrastructure.adapter.out.event;

import com.template.api.shared.domain.event.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("SpringEventPublisherAdapter Unit Tests")
class SpringEventPublisherAdapterTest {

    record DummyEvent(UUID eventId, String aggregateId, Instant occurredAt, String eventType) implements DomainEvent {}

    @Test
    @DisplayName("Should throw NullPointerException when publisher is null")
    void constructor_nullPublisher_shouldThrow() {
        assertThatThrownBy(() -> new SpringEventPublisherAdapter(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("publisher cannot be null");
    }

    @Test
    @DisplayName("Should publish domain event to ApplicationEventPublisher")
    void publish_validEvent_shouldDelegateToPublisher() {
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        SpringEventPublisherAdapter adapter = new SpringEventPublisherAdapter(publisher);

        DomainEvent event = new DummyEvent(UUID.randomUUID(), "agg-1", Instant.now(), "dummy.v1");
        adapter.publish(event);

        verify(publisher).publishEvent(event);
    }

    @Test
    @DisplayName("Should silently ignore null event in publish")
    void publish_nullEvent_shouldNotThrowOrPublish() {
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        SpringEventPublisherAdapter adapter = new SpringEventPublisherAdapter(publisher);

        adapter.publish(null);

        verify(publisher, never()).publishEvent(Mockito.any());
    }

    @Test
    @DisplayName("Should publish all non-null domain events in publishAll")
    void publishAll_validList_shouldPublishAll() {
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        SpringEventPublisherAdapter adapter = new SpringEventPublisherAdapter(publisher);

        DomainEvent event1 = new DummyEvent(UUID.randomUUID(), "agg-1", Instant.now(), "event.1");
        DomainEvent event2 = new DummyEvent(UUID.randomUUID(), "agg-2", Instant.now(), "event.2");

        adapter.publishAll(List.of(event1, event2));

        verify(publisher).publishEvent(event1);
        verify(publisher).publishEvent(event2);
    }

    @Test
    @DisplayName("Should ignore null or empty lists, and skip null elements in publishAll")
    void publishAll_edgeCases_shouldHandleGracefully() {
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        SpringEventPublisherAdapter adapter = new SpringEventPublisherAdapter(publisher);

        adapter.publishAll(null);
        adapter.publishAll(List.of());

        DomainEvent validEvent = new DummyEvent(UUID.randomUUID(), "agg-1", Instant.now(), "event.valid");
        adapter.publishAll(Arrays.asList(null, validEvent, null));

        verify(publisher, Mockito.times(1)).publishEvent(validEvent);
    }
}
