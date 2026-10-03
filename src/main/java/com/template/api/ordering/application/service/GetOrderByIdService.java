package com.template.api.ordering.application.service;

import com.template.api.ordering.application.port.in.GetOrderByIdUseCase;
import com.template.api.ordering.application.port.out.OrderQueryPort;
import com.template.api.ordering.application.query.GetOrderByIdQuery;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.domain.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating the read-only query flow directly to DTO projection (CQRS).
 * Conforms to APP-01, OUT-04, and TRX-01.
 */
@Service
public class GetOrderByIdService implements GetOrderByIdUseCase {

    private final OrderQueryPort orderQueryPort;

    public GetOrderByIdService(OrderQueryPort orderQueryPort) {
        this.orderQueryPort = orderQueryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResult execute(GetOrderByIdQuery query) {
        return orderQueryPort.findOrderResultById(query.orderId())
                .orElseThrow(() -> new ResourceNotFoundException(Order.class, query.orderId()));
    }
}
