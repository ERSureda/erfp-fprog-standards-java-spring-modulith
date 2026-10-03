package com.template.api.ordering.application.service;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import com.template.api.ordering.application.port.in.ProcessOrderPaymentUseCase;
import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating payment confirmation updates from asynchronous workers.
 * Conforms to APP-01, APP-02, and TRX-01.
 */
@Service
@RequiredArgsConstructor
public class ProcessOrderPaymentService implements ProcessOrderPaymentUseCase {

    private final OrderRepositoryPort orderRepository;

    @Override
    @Transactional
    public void execute(ProcessOrderPaymentCommand command) {
        Order order = orderRepository.findById(command.orderId()).orElse(null);
        if (order != null && "CONFIRMED".equalsIgnoreCase(command.paymentStatus())) {
            order.confirm();
            orderRepository.save(order);
        }
    }
}
