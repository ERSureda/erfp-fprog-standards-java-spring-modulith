package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.mapper;

import com.template.api.ordering.domain.model.Money;
import com.template.api.ordering.domain.model.Order;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.entity.OrderEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderPersistenceMapper Unit Tests")
class OrderPersistenceMapperTest {

    private OrderPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrderPersistenceMapper() {};
    }

    @Test
    @DisplayName("should_MapDomainOrderToEntity_Correctly")
    void should_MapDomainOrderToEntity_Correctly() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Order order = Order.create(customerId, Money.of(new BigDecimal("150.00"), Currency.getInstance("USD")));

        // Act
        OrderEntity entity = mapper.toEntity(order);

        // Assert
        assertThat(entity.getId()).isEqualTo(order.getId());
        assertThat(entity.getCustomerId()).isEqualTo(customerId);
        assertThat(entity.getAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(entity.getCurrency()).isEqualTo("USD");
        assertThat(entity.getStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(entity.getVersion()).isEqualTo(0L);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("should_MapEntityToDomainOrder_Correctly")
    void should_MapEntityToDomainOrder_Correctly() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();
        OrderEntity entity = new OrderEntity(
                orderId,
                customerId,
                new BigDecimal("99.99"),
                "EUR",
                "CONFIRMED",
                1L,
                now,
                now
        );

        // Act
        Order order = mapper.toDomain(entity);

        // Assert
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getAmount().amount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(order.getAmount().currency().getCurrencyCode()).isEqualTo("EUR");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getVersion()).isEqualTo(1L);
        assertThat(order.getCreatedAt()).isEqualTo(now);
        assertThat(order.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("should_ReturnNull_when_InputIsNull")
    void should_ReturnNull_when_InputIsNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
