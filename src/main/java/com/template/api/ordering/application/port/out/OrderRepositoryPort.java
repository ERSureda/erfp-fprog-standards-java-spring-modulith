package com.template.api.ordering.application.port.out;

import com.template.api.ordering.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary outbound port for persisting and retrieving Order aggregate roots.
 * <p>
 * Defines the transactional boundary contract between application services and persistence adapters.
 * Conforms to OUT-01 and TRX-02.
 */
public interface OrderRepositoryPort {

    Order save(Order order);
    Optional<Order> findById(UUID id);
}
