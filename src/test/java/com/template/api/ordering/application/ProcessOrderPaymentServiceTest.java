package com.template.api.ordering.application;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.application.service.ProcessOrderPaymentService;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.domain.valueobject.Money;
import com.template.api.ordering.domain.model.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test suite for {@link ProcessOrderPaymentService}.
 * <p>
 * Verifies asynchronous payment handling, state transition invocation, and non-existing order handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessOrderPaymentService Application Unit Tests")
class ProcessOrderPaymentServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @InjectMocks
    private ProcessOrderPaymentService processOrderPaymentService;

    @Test
    @DisplayName("should_ConfirmOrder_when_PaymentStatusIsConfirmed")
    void should_ConfirmOrder_when_PaymentStatusIsConfirmed() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Order order = Order.create(orderId, customerId, Money.of(new BigDecimal("120.00"), Currency.getInstance("EUR")));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        processOrderPaymentService.execute(new ProcessOrderPaymentCommand(orderId, "CONFIRMED"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("should_DoNothing_when_OrderNotFound")
    void should_DoNothing_when_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        processOrderPaymentService.execute(new ProcessOrderPaymentCommand(orderId, "CONFIRMED"));

        verify(orderRepository, never()).save(any());
    }
}
