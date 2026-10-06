package com.template.api.ordering.application.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test suite for {@link ProcessOrderPaymentCommand}.
 * <p>
 * Verifies defensive zero-allocation fail-fast null checks in compact constructor per APP-03 and ADR-006.
 */
@DisplayName("ProcessOrderPaymentCommand Unit Tests")
class ProcessOrderPaymentCommandTest {

    @Test
    @DisplayName("should create command when valid parameters are provided")
    void shouldCreateCommandWhenValidParametersProvided() {
        UUID orderId = UUID.randomUUID();
        String status = "CONFIRMED";

        ProcessOrderPaymentCommand command = new ProcessOrderPaymentCommand(orderId, status);

        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.paymentStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("should throw NullPointerException when orderId is null")
    void shouldThrowNullPointerExceptionWhenOrderIdIsNull() {
        assertThatThrownBy(() -> new ProcessOrderPaymentCommand(null, "CONFIRMED"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("ORDER_ID_CANNOT_BE_NULL");
    }

    @Test
    @DisplayName("should throw NullPointerException when paymentStatus is null")
    void shouldThrowNullPointerExceptionWhenPaymentStatusIsNull() {
        UUID orderId = UUID.randomUUID();
        assertThatThrownBy(() -> new ProcessOrderPaymentCommand(orderId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("PAYMENT_STATUS_CANNOT_BE_NULL");
    }
}
