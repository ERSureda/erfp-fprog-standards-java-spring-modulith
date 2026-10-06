package com.template.api.ordering.domain.model;

import com.template.api.ordering.domain.OrderingError;
import com.template.api.ordering.domain.event.OrderCancelledEvent;
import com.template.api.ordering.domain.event.OrderConfirmedEvent;
import com.template.api.ordering.domain.event.OrderCreatedEvent;
import com.template.api.ordering.domain.event.OrderShippedEvent;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.shared.domain.exception.ConflictException;
import com.template.api.shared.domain.model.AggregateRoot;
import com.template.api.shared.domain.valueobject.Money;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Order Aggregate Root maintaining business invariants, state transitions, and domain events.
 * <p>
 * Encapsulates the complete lifecycle of customer orders and emits events upon state mutations.
 * Enforces non-public constructors and immutable identity management.
 * Conforms to DOM-01, DOM-02, DOM-03, DOM-04, DOM-05, and TRX-02.
 */
public class Order extends AggregateRoot<UUID> {

    private final UUID customerId;
    private Money amount;
    private OrderStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    // --- Constructors & Factories ---
    private Order(
            UUID id,
            UUID customerId,
            Money amount,
            OrderStatus status,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, version);
        this.customerId = Objects.requireNonNull(customerId, "ORDER_CUSTOMER_ID_CANNOT_BE_NULL");
        this.amount = Objects.requireNonNull(amount, "ORDER_AMOUNT_CANNOT_BE_NULL");
        this.status = Objects.requireNonNull(status, "ORDER_STATUS_CANNOT_BE_NULL");
        this.createdAt = Objects.requireNonNull(createdAt, "ORDER_CREATED_AT_CANNOT_BE_NULL");
        this.updatedAt = Objects.requireNonNull(updatedAt, "ORDER_UPDATED_AT_CANNOT_BE_NULL");
    }

    public static Order create(UUID id, UUID customerId, Money amount) {
        Instant now = Instant.now();
        Order order = new Order(id, customerId, amount, OrderStatus.PENDING, 0L, now, now);
        order.registerEvent(new OrderCreatedEvent(
                id,
                customerId,
                amount.amount(),
                amount.currency().getCurrencyCode()
        ));
        return order;
    }

    public static Order reconstruct(
            UUID id,
            UUID customerId,
            Money amount,
            OrderStatus status,
            Long version,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Order(id, customerId, amount, status, version, createdAt, updatedAt);
    }

    // --- Business Logic ---
    public boolean canConfirm() {
        return this.status == OrderStatus.PENDING;
    }

    public void confirm() {
        if (!canConfirm()) {
            throw new ConflictException(OrderingError.ORDERING_ORDER_INVALID_STATUS);
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
        this.registerEvent(new OrderConfirmedEvent(this.id, this.customerId));
    }

    public boolean canShip() {
        return this.status == OrderStatus.CONFIRMED;
    }

    public void ship() {
        if (!canShip()) {
            throw new ConflictException(OrderingError.ORDERING_ORDER_NOT_CONFIRMED);
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
        this.registerEvent(new OrderShippedEvent(this.id, this.customerId));
    }

    public boolean canCancel() {
        return this.status != OrderStatus.SHIPPED && this.status != OrderStatus.CANCELLED;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new ConflictException(OrderingError.ORDERING_ORDER_ALREADY_SHIPPED);
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.registerEvent(new OrderCancelledEvent(this.id, this.customerId));
    }

    // --- Getters ---
    public UUID getCustomerId() {
        return customerId;
    }

    public Money getAmount() {
        return amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
