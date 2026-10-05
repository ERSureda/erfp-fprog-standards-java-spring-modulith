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
 * Secondary outbound persistence adapter implementing {@link OrderRepositoryPort} using Spring Data JPA.
 * <p>
 * Manages JPA entity transactions and atomically publishes domain events via {@link OutboxPublisherPort}.
 * Conforms to OUT-01, TRX-02, and TRX-03.
 */
@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper orderPersistenceMapper;
    private final OutboxPublisherPort outboxPublisherPort;

    @Override
    public Optional<Order> findById(UUID id) {
        return orderJpaRepository.findById(id).map(orderPersistenceMapper::toDomain);
    }

    @Override
    public Order save(Order order) {
        OrderEntity saved = orderJpaRepository.save(orderPersistenceMapper.toEntity(order));
        if (order.hasDomainEvents()) {
            outboxPublisherPort.publishAll(order.pullDomainEvents());
        }
        return orderPersistenceMapper.toDomain(saved);
    }
}
