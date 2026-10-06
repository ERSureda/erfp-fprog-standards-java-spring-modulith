package com.template.api.ordering.application;

import com.template.api.ordering.application.command.CreateOrderCommand;
import com.template.api.ordering.application.port.out.OrderRepositoryPort;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.application.service.CreateOrderService;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.application.port.out.ExecutionContextPort;
import com.template.api.shared.application.port.out.UuidGeneratorPort;
import com.template.api.shared.domain.exception.ValidationException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test suite for {@link CreateOrderService}.
 * <p>
 * Verifies application service orchestration, tenant/user context resolution, UUIDv7 generation, and persistence delegation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOrderService Application Unit Tests")
class CreateOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private ExecutionContextPort executionContextPort;

    @Mock
    private UuidGeneratorPort uuidGeneratorPort;

    @InjectMocks
    private CreateOrderService createOrderService;

    @Test
    @DisplayName("should_CreateOrderAndPersist_when_CommandIsValid")
    void should_CreateOrderAndPersist_when_CommandIsValid() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID generatedOrderId = UUID.randomUUID();
        when(executionContextPort.current()).thenReturn(
                new ExecutionContext(tenantId, userId, Set.of("USER"))
        );
        when(uuidGeneratorPort.generateId()).thenReturn(generatedOrderId);

        CreateOrderCommand command = new CreateOrderCommand(new BigDecimal("150.00"), "EUR");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResult result = createOrderService.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(generatedOrderId);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(result.currency()).isEqualTo("EUR");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("should_GenerateUuidv7ForOrderAndCustomer_when_CustomerIdIsNull")
    void should_GenerateUuidv7ForOrderAndCustomer_when_CustomerIdIsNull() {
        UUID generatedCustomerId = UUID.randomUUID();
        UUID generatedOrderId = UUID.randomUUID();
        when(executionContextPort.current()).thenReturn(null);
        when(uuidGeneratorPort.generateId()).thenReturn(generatedCustomerId, generatedOrderId);

        CreateOrderCommand command = new CreateOrderCommand(new BigDecimal("99.00"), "EUR");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResult result = createOrderService.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(generatedOrderId);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("should_ThrowValidationException_when_CurrencyCodeIsInvalid")
    void should_ThrowValidationException_when_CurrencyCodeIsInvalid() {
        CreateOrderCommand command = new CreateOrderCommand(new BigDecimal("50.00"), "INVALID_CURRENCY");

        assertThatThrownBy(() -> createOrderService.execute(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ORDER_CURRENCY_INVALID_CODE");
    }
}
