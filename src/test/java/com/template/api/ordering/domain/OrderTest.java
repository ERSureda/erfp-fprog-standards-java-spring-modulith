package com.template.api.ordering.domain;

import com.template.api.ordering.domain.event.OrderCancelledEvent;
import com.template.api.ordering.domain.event.OrderConfirmedEvent;
import com.template.api.ordering.domain.event.OrderCreatedEvent;
import com.template.api.ordering.domain.event.OrderShippedEvent;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.domain.valueobject.Money;
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

/**
 * Test suite for {@link Order} Aggregate Root.
 * <p>
 * Verifies domain invariants, state transitions (PENDING -> CONFIRMED -> SHIPPED / CANCELLED),
 * and domain event registration.
 */
@DisplayName("Order Aggregate & Value Object Domain Tests")
class OrderTest {

    @Test
    @DisplayName("should_RegisterOrderCreatedEvent_when_OrderIsCreated")
    void should_RegisterOrderCreatedEvent_when_OrderIsCreated() {
        UUID customerId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("150.00"), Currency.getInstance("EUR"));

        Order order = Order.create(customerId, amount);

        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomerId()).isEqualTo(customerId);
        assertThat(order.getAmount()).isEqualTo(amount);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.canConfirm()).isTrue();
        assertThat(order.canShip()).isFalse();
        assertThat(order.canCancel()).isTrue();
        assertThat(order.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrderCreatedEvent.class);
    }

    @Test
    @DisplayName("should_ThrowValidationException_when_AmountIsNegative")
    void should_ThrowValidationException_when_AmountIsNegative() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-10.00"), Currency.getInstance("EUR")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should_ThrowNullPointerException_when_CustomerIdIsNullInCreation")
    void should_ThrowNullPointerException_when_CustomerIdIsNullInCreation() {
        Money amount = Money.of(new BigDecimal("50.00"), Currency.getInstance("EUR"));

        assertThatThrownBy(() -> Order.create(null, amount))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("customerId cannot be null");
    }

    @Test
    @DisplayName("should_ThrowNullPointerException_when_AmountIsNullInCreation")
    void should_ThrowNullPointerException_when_AmountIsNullInCreation() {
        assertThatThrownBy(() -> Order.create(UUID.randomUUID(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount cannot be null");
    }

    @Test
    @DisplayName("should_TransitionStatusAndRegisterEvents_when_ValidBusinessActionsAreExecuted")
    void should_TransitionStatusAndRegisterEvents_when_ValidBusinessActionsAreExecuted() {
        Order order = Order.create(UUID.randomUUID(), Money.of(new BigDecimal("99.99"), Currency.getInstance("EUR")));
        order.pullDomainEvents();

        order.confirm();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.canConfirm()).isFalse();
        assertThat(order.canShip()).isTrue();
        assertThat(order.canCancel()).isTrue();
        assertThat(order.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrderConfirmedEvent.class);

        order.ship();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.canConfirm()).isFalse();
        assertThat(order.canShip()).isFalse();
        assertThat(order.canCancel()).isFalse();
        assertThat(order.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrderShippedEvent.class);

        assertThatThrownBy(order::cancel)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot cancel an order in status: SHIPPED");
    }

    @Test
    @DisplayName("should_CancelOrderAndRegisterEvent_when_OrderIsPending")
    void should_CancelOrderAndRegisterEvent_when_OrderIsPending() {
        Order order = Order.create(UUID.randomUUID(), Money.of(new BigDecimal("99.99"), Currency.getInstance("EUR")));
        order.pullDomainEvents();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.canConfirm()).isFalse();
        assertThat(order.canShip()).isFalse();
        assertThat(order.canCancel()).isFalse();
        assertThat(order.pullDomainEvents())
                .hasSize(1)
                .first()
                .isInstanceOf(OrderCancelledEvent.class);
    }

    @Test
    @DisplayName("should_ThrowConflictException_when_ConfirmingCancelledOrder")
    void should_ThrowConflictException_when_ConfirmingCancelledOrder() {
        Order order = Order.create(UUID.randomUUID(), Money.of(new BigDecimal("99.99"), Currency.getInstance("EUR")));
        order.cancel();

        assertThatThrownBy(order::confirm)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Order cannot be confirmed from status: CANCELLED");
    }

    @Test
    @DisplayName("should_ThrowConflictException_when_ShippingUnconfirmedOrder")
    void should_ThrowConflictException_when_ShippingUnconfirmedOrder() {
        Order order = Order.create(UUID.randomUUID(), Money.of(new BigDecimal("99.99"), Currency.getInstance("EUR")));

        assertThatThrownBy(order::ship)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Order must be confirmed before shipping. Current status: PENDING");
    }
}
