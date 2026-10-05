package com.template.api.ordering.application.service;

import com.template.api.ordering.application.command.CreateOrderCommand;
import com.template.api.ordering.application.port.in.CreateOrderUseCase;
import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.domain.valueobject.Money;
import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.application.port.out.ExecutionContextPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.UUID;

/**
 * Application service orchestrating the order creation command workflow.
 * <p>
 * Enforces transactional demarcation, infers caller identity from execution context,
 * instantiates the Order aggregate root, and persists changes through outbound ports.
 * Conforms to APP-01, APP-02, and TRX-01.
 */
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final ExecutionContextPort executionContextPort;

    @Override
    @Transactional
    public OrderResult execute(CreateOrderCommand command) {
        ExecutionContext context = executionContextPort != null ? executionContextPort.current() : null;
        UUID customerId = command.customerId();
        if (customerId == null && context != null && context.userId() != null) {
            customerId = context.userId();
        }
        if (customerId == null) {
            customerId = UUID.randomUUID();
        }

        Currency currency = Currency.getInstance(command.currency() != null ? command.currency() : "EUR");
        Money money = Money.of(command.amount(), currency);

        Order order = Order.create(customerId, money);
        Order savedOrder = orderRepository.save(order);

        return new OrderResult(
                savedOrder.getId(),
                savedOrder.getStatus().name(),
                savedOrder.getAmount().amount(),
                savedOrder.getAmount().currency().getCurrencyCode()
        );
    }
}
