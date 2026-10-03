package com.template.api.ordering.application.port.out;

import com.template.api.ordering.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary port for saving and retrieving Order aggregates.
 */
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(UUID id);
}
