package com.template.api.ordering.domain.model;

import com.template.api.ordering.domain.OrderingError;
import com.template.api.ordering.domain.event.OrderCreatedEvent;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import com.template.api.shared.domain.exception.ConflictException;
import com.template.api.shared.domain.model.AggregateRoot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Order Aggregate Root maintaining state transitions, business invariants, and domain events.
 * Conforms to DOM-01, DOM-02, DOM-03, DOM-04, DOM-05, and TRX-02.
 */
public class Order extends AggregateRoot<UUID> {

    private UUID customerId;
    private Money amount;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    protected Order() {
        super();
    }

    protected Order(UUID id, UUID customerId, Money amount, OrderStatus status, Long version, Instant createdAt, Instant updatedAt) {
        super(id, version);
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.amount = Objects.requireNonNull(amount, "amount cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Order create(UUID customerId, Money amount) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Order order = new Order(id, customerId, amount, OrderStatus.PENDING, 0L, now, now);
        order.registerEvent(new OrderCreatedEvent(id, customerId, amount.amount(), amount.currency().getCurrencyCode()));
        return order;
    }

    public static Order reconstruct(UUID id, UUID customerId, Money amount, OrderStatus status, Long version, Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, amount, status, version, createdAt, updatedAt);
    }

    public void confirm() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new ConflictException(OrderingError.ORDER_ALREADY_SHIPPED, "Cannot confirm a cancelled order");
        }
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void ship() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new ConflictException(OrderingError.ORDER_ALREADY_SHIPPED, "Order must be confirmed before shipping");
        }
        this.status = OrderStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        if (this.status == OrderStatus.SHIPPED) {
            throw new ConflictException(OrderingError.ORDER_ALREADY_SHIPPED, "Cannot cancel an order that has already shipped");
        }
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

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
