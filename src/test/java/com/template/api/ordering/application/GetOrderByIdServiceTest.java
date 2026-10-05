package com.template.api.ordering.application;

import com.template.api.ordering.application.port.out.OrderQueryPort;
import com.template.api.ordering.application.query.GetOrderByIdQuery;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.application.service.GetOrderByIdService;
import com.template.api.shared.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test suite for {@link GetOrderByIdService}.
 * <p>
 * Verifies CQRS query delegation and missing entity exception mapping.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetOrderByIdService Application Unit Tests")
class GetOrderByIdServiceTest {

    @Mock
    private OrderQueryPort orderQueryPort;

    @InjectMocks
    private GetOrderByIdService getOrderByIdService;

    @Test
    @DisplayName("should_ReturnOrderResult_when_OrderExists")
    void should_ReturnOrderResult_when_OrderExists() {
        UUID orderId = UUID.randomUUID();
        OrderResult expected = new OrderResult(orderId, "PENDING", new BigDecimal("89.99"), "EUR");
        when(orderQueryPort.findOrderResultById(orderId)).thenReturn(Optional.of(expected));

        OrderResult actual = getOrderByIdService.execute(new GetOrderByIdQuery(orderId));

        assertThat(actual).isNotNull();
        assertThat(actual.id()).isEqualTo(orderId);
        assertThat(actual.status()).isEqualTo("PENDING");
        assertThat(actual.amount()).isEqualByComparingTo(new BigDecimal("89.99"));
    }

    @Test
    @DisplayName("should_ThrowResourceNotFoundException_when_OrderNotFound")
    void should_ThrowResourceNotFoundException_when_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderQueryPort.findOrderResultById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getOrderByIdService.execute(new GetOrderByIdQuery(orderId)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(orderId.toString());
    }
}
