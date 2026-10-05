package com.template.api.ordering.application.port.out;

import com.template.api.ordering.application.result.OrderResult;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary port for lightweight read-only queries directly returning OrderResult (CQRS).
 */
public interface OrderQueryPort {

    Optional<OrderResult> findOrderResultById(UUID id);
}
