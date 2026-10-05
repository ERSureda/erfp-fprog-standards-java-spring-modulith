package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.mapper;

import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.domain.valueobject.Money;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.entity.OrderEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Infrastructure mapping component translating between {@link Order} aggregates and {@link OrderEntity} records.
 * <p>
 * Encapsulates bidirectional entity transformations to maintain domain model purity.
 * Conforms to DOM-01 and OUT-01.
 */
@Component
public class OrderPersistenceMapper {

    public OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }

        BigDecimal amount = domain.getAmount() != null ? domain.getAmount().amount() : null;
        String currency = (domain.getAmount() != null && domain.getAmount().currency() != null)
                ? domain.getAmount().currency().getCurrencyCode()
                : null;
        String status = domain.getStatus() != null ? domain.getStatus().name() : null;

        return new OrderEntity(
                domain.getId(),
                domain.getCustomerId(),
                amount,
                currency,
                status,
                domain.getVersion(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

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
