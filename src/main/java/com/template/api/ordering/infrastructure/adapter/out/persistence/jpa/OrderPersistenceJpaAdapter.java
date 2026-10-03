package com.template.api.ordering.infrastructure.adapter.out.persistence.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.domain.model.Money;
import com.template.api.ordering.domain.model.Order;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.shared.domain.event.DomainEvent;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter implementing OrderRepositoryPort using Spring Data JPA and relational mapping.
 * Also persists aggregate domain events into the transactional outbox (TRX-03).
 */
@Repository
public class OrderPersistenceJpaAdapter implements OrderRepositoryPort {

    private static final String OUTBOX_INSERT_SQL = """
        INSERT INTO outbox_events (
            event_id, tenant_id, aggregate_type, aggregate_id,
            event_type, payload, correlation_id, status
        ) VALUES (
            :eventId, :tenantId, :aggType, :aggId,
            :eventType, CAST(:payload AS jsonb), :corrId, 'PENDING'
        )
    """;

    private final SpringDataOrderRepository repository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderPersistenceJpaAdapter(
            SpringDataOrderRepository repository,
            EntityManager entityManager,
            @Autowired(required = false) ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
        this.objectMapper = (objectMapper != null)
                ? objectMapper
                : new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public OrderPersistenceJpaAdapter(SpringDataOrderRepository repository, EntityManager entityManager) {
        this(repository, entityManager, null);
    }

    @Override
    public Order save(Order order) {
        List<DomainEvent> domainEvents = order.pullDomainEvents();

        OrderJpaEntity entity = toEntity(order);
        OrderJpaEntity saved = repository.save(entity);

        // Transactional Outbox (TRX-03): Persist domain events atomically in the same transaction
        persistOutboxEvents(domainEvents);

        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    private void persistOutboxEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty() || entityManager == null) {
            return;
        }

        for (DomainEvent event : events) {
            try {
                String payload = objectMapper.writeValueAsString(event);

                entityManager.createNativeQuery(OUTBOX_INSERT_SQL)
                        .setParameter("eventId", event.eventId())
                .setParameter("tenantId", ExecutionContextHolder.getTenantId())
                .setParameter("aggType", "Order")
                .setParameter("aggId", event.aggregateId())
                .setParameter("eventType", event.getClass().getName())
                .setParameter("payload", payload)
                .setParameter("corrId", ExecutionContextHolder.get() != null ? ExecutionContextHolder.get().correlationId() : null)
                .executeUpdate();
            } catch (Exception ex) {
                throw new InfrastructureException("Failed to persist domain event to outbox", ex);
            }
        }
    }

    private OrderJpaEntity toEntity(Order order) {
        return new OrderJpaEntity(
                order.getId(),
                order.getCustomerId(),
                order.getAmount().amount(),
                order.getAmount().currency().getCurrencyCode(),
                order.getStatus().name(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private Order toDomain(OrderJpaEntity entity) {
        Money money = Money.of(entity.getAmount(), Currency.getInstance(entity.getCurrency()));
        OrderStatus status = OrderStatus.valueOf(entity.getStatus());
        return Order.reconstruct(
                entity.getId(),
                entity.getCustomerId(),
                money,
                status,
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
