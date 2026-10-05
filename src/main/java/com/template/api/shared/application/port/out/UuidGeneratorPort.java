package com.template.api.shared.application.port.out;

import java.util.UUID;

/**
 * Outbound port defining unique identifier generation strategies.
 * <p>
 * Provides sequential time-ordered identifiers (RFC 9562 UUIDv7) to optimize B-Tree index insertions.
 * Conforms to SED-02.
 */
public interface UuidGeneratorPort {

    UUID generateId();

    default String generateIdString() {
        return generateId().toString();
    }
}
