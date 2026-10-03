package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.adapter;

import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.domain.model.Order;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.entity.OrderEntity;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.mapper.OrderPersistenceMapper;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.repository.OrderJpaRepository;
import com.template.api.shared.application.port.out.OutboxPublisherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter implementing OrderRepositoryPort using Spring Data JPA.
 * Conforms to OUT-01 and TRX-03.
 */
@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository repository;
    private final OrderPersistenceMapper mapper;
    private final OutboxPublisherPort outboxPublisherPort;

    @Override
    public Order save(Order order) {
        OrderEntity saved = repository.save(mapper.toEntity(order));
        if (order.hasDomainEvents()) {
            outboxPublisherPort.publishAll(order.pullDomainEvents());
        }
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
