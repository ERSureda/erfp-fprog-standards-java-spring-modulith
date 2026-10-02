package com.template.api.shared.application.port.out;

import java.util.UUID;

/**
 * Outbound port defining unique identifier generation strategies.
 * <p>
 * Isolates ID generation mechanics (such as time-ordered UUIDv7 or standard UUIDv4)
 * from application workflows, allowing deterministic stubbing in tests and optimal
 * sequential index insertion in database engines.
 */
public interface UuidGeneratorPort {

    UUID generateId();

    default String generateIdString() {
        return generateId().toString();
    }
}
