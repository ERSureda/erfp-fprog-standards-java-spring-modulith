package com.template.api.shared.domain.model;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base abstract class for Aggregate Roots in Domain-Driven Design (DDD).
 * <p>
 * Establishes a transactional consistency boundary for an entity cluster.
 * Encapsulates the recording and atomic draining of domain events emitted during state transitions,
 * and maintains an optimistic locking version counter.
 * Conforms to DOM-01, DOM-02, DOM-03, and TRX-02.
 *
 * @param <ID> unique identifier type of the aggregate root
 */
public abstract class AggregateRoot<ID> extends BaseEntity<ID> {

    protected Long version;
    private transient List<DomainEvent> domainEvents;

    protected AggregateRoot() {
        super();
    }

    protected AggregateRoot(ID id) {
        super(id);
    }

    protected AggregateRoot(ID id, Long version) {
        super(id);
        this.version = version;
    }

    public Long version() {
        return version;
    }

    public Long getVersion() {
        return version();
    }

    public boolean hasDomainEvents() {
        return domainEvents != null && !domainEvents.isEmpty();
    }

    protected void registerEvent(DomainEvent event) {
        Objects.requireNonNull(event, "AGGREGATE_EVENT_CANNOT_BE_NULL");
        if (this.domainEvents == null) {
            this.domainEvents = new ArrayList<>(2);
        }
        this.domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        if (this.domainEvents == null || this.domainEvents.isEmpty()) {
            return List.of();
        }
        List<DomainEvent> events = List.copyOf(this.domainEvents);
        this.domainEvents = null;
        return events;
    }
}
