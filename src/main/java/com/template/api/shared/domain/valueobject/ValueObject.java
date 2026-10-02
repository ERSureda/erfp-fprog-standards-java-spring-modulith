package com.template.api.shared.domain.valueobject;

/**
 * Marker interface identifying Value Objects within the domain model.
 * <p>
 * In Domain-Driven Design (DDD), a Value Object is an immutable conceptual whole defined
 * purely by its attributes rather than a persistent identity or thread of continuity.
 * In modern Java, implementations should be declared as {@code record} types to guarantee
 * immutability, structural equality (equals and hashCode), and transparent component access.
 * Also serves as an architectural hook for ArchUnit governance rules and persistence mappers.
 */
public interface ValueObject {
}