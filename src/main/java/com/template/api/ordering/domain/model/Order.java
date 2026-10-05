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
 * Order Aggregate Root maintaining state transitions, business invariants, and domain events.
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
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
    }

    public static Order create(UUID customerId, Money amount) {
        return create(UUID.randomUUID(), customerId, amount);
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
            throw new ConflictException(OrderingError.ORDER_INVALID_STATUS,
                    "Order cannot be confirmed from status: " + this.status);
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
            throw new ConflictException(OrderingError.ORDER_NOT_CONFIRMED,
                    "Order must be confirmed before shipping. Current status: " + this.status);
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
            throw new ConflictException(OrderingError.ORDER_ALREADY_SHIPPED,
                    "Cannot cancel an order in status: " + this.status);
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
