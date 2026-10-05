package com.template.api.ordering.application.port.in;

import com.template.api.ordering.application.command.CreateOrderCommand;
import com.template.api.ordering.application.result.OrderResult;

/**
 * Primary inbound port defining the contract for order creation.
 * <p>
 * Implemented by application services to execute the order placement command flow.
 * Conforms to APP-01 and APP-02.
 */
public interface CreateOrderUseCase {

    OrderResult execute(CreateOrderCommand command);
}
