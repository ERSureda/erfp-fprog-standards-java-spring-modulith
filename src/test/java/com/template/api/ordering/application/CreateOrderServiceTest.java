package com.template.api.ordering.application;

import com.template.api.ordering.application.command.CreateOrderCommand;
import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.application.service.CreateOrderService;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.application.port.out.ExecutionContextPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test suite for {@link CreateOrderService}.
 * <p>
 * Verifies application service orchestration, tenant/user context resolution, and persistence delegation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderService Application Unit Tests")
class CreateOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private ExecutionContextPort executionContextPort;

    @InjectMocks
    private CreateOrderService createOrderService;

    @Test
    @DisplayName("should_CreateOrderAndPersist_when_CommandIsValid")
    void should_CreateOrderAndPersist_when_CommandIsValid() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(executionContextPort.current()).thenReturn(
                new ExecutionContext(tenantId, userId, Set.of("USER"))
        );

        CreateOrderCommand command = new CreateOrderCommand(new BigDecimal("150.00"), "EUR");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResult result = createOrderService.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.currency()).isEqualTo("EUR");
        verify(orderRepository).save(any(Order.class));
    }
}
