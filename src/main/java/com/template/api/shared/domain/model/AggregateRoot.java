package com.template.api.shared.domain.model;

import com.template.api.shared.domain.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Base abstract class for Aggregate Roots in Domain-Driven Design (DDD).
 * <p>
 * An Aggregate Root establishes a transactional boundary for a cluster of associated domain objects.
 * It encapsulates the recording and lifecycle of domain events produced by state transitions,
 * ensuring they are only drained and published upon successful persistence. Also encapsulates
 * an optional optimistic locking version indicator for concurrency control.
 *
 * @param <ID> type of the aggregate root unique identifier
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
        Objects.requireNonNull(event, "event cannot be null");
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