package com.template.api.ordering.application.port.out;

import com.template.api.ordering.application.result.OrderResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary outbound port for lightweight read-only queries directly returning OrderResult DTOs.
 * <p>
 * Bypasses domain aggregate rehydration and ORM entity lifecycle for optimal read performance (CQRS).
 * Conforms to OUT-04.
 */
public interface OrderQueryPort {

    Optional<OrderResult> findOrderResultById(UUID id);
}
