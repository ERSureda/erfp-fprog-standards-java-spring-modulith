package com.template.api.shared.domain.model;

import java.util.Objects;

/**
 * Base abstract class for domain entities in Domain-Driven Design (DDD).
 * <p>
 * An entity represents an individual domain concept defined by its thread of continuity
 * and persistent identity rather than its structural attributes. Enforces strict identity-based
 * equality where two entities of the same type sharing an identical ID are considered equal.
 *
 * @param <ID> type of the entity unique identifier
 */
public abstract class BaseEntity<ID> {

    protected ID id;

    protected BaseEntity() {}

    protected BaseEntity(ID id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
    }

    public ID id() {
        return id;
    }

    public ID getId() {
        return id();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity<?> that = (BaseEntity<?>) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}