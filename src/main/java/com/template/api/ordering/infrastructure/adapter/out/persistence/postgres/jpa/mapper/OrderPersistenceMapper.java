package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.mapper;

import com.template.api.ordering.domain.model.Money;
import com.template.api.ordering.domain.model.Order;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.Currency;

/**
 * MapStruct mapper between domain aggregate {@link Order} and persistence {@link OrderEntity}.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface OrderPersistenceMapper {

    default OrderEntity toEntity(Order domain) {
        if (domain == null) {
            return null;
        }
        OrderEntity entity = new OrderEntity();
        entity.setId(domain.getId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setAmount(domain.getAmount() != null ? domain.getAmount().amount() : null);
        entity.setCurrency(domain.getAmount() != null && domain.getAmount().currency() != null
                ? domain.getAmount().currency().getCurrencyCode() : null);
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setVersion(domain.getVersion());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    default Order toDomain(OrderEntity entity) {
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
