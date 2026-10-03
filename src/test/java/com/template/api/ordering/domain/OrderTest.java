package com.template.api.ordering.domain;

import com.template.api.ordering.domain.event.OrderCreatedEvent;
import com.template.api.ordering.domain.model.Money;
import com.template.api.ordering.domain.model.Order;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.shared.domain.exception.ConflictException;
import com.template.api.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order Aggregate & Value Object Domain Tests")
class OrderTest {

    @Test
    @DisplayName("should_RegisterOrderCreatedEvent_when_OrderIsCreated")
    void should_RegisterOrderCreatedEvent_when_OrderIsCreated() {
        // Arrange
        UUID customerId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("150.00"), Currency.getInstance("EUR"));

        // Act
        Order order = Order.create(customerId, amount);

        // Assert
        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrderCreatedEvent.class);
    }

    @Test
    @DisplayName("should_ThrowValidationException_when_AmountIsNegative")
    void should_ThrowValidationException_when_AmountIsNegative() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> Money.of(new BigDecimal("-10.00"), Currency.getInstance("EUR")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should_TransitionStatus_when_ValidBusinessActionsAreExecuted")
    void should_TransitionStatus_when_ValidBusinessActionsAreExecuted() {
        // Arrange
        Order order = Order.create(UUID.randomUUID(), Money.of(new BigDecimal("99.99"), Currency.getInstance("EUR")));

        // Act & Assert transitions
        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        order.ship();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);

        assertThatThrownBy(order::cancel)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot cancel an order that has already shipped");
    }
}
