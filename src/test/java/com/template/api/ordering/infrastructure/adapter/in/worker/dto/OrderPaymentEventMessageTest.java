package com.template.api.ordering.infrastructure.adapter.in.worker.dto;

import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for {@link OrderPaymentEventMessage}.
 * <p>
 * Verifies transformation to application command with explicit and default status values.
 */
@DisplayName("OrderPaymentEventMessage Unit Tests")
class OrderPaymentEventMessageTest {

    @Test
    @DisplayName("toCommand should map fields into ProcessOrderPaymentCommand with provided status")
    void toCommand_withExplicitStatus_shouldMapFields() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentEventMessage message = new OrderPaymentEventMessage(orderId, "FAILED");

        ProcessOrderPaymentCommand command = message.toCommand();

        assertThat(command).isNotNull();
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.paymentStatus()).isEqualTo("FAILED");
    }

    @Test
    @DisplayName("toCommand should default paymentStatus to CONFIRMED when null")
    void toCommand_withNullStatus_shouldDefaultToConfirmed() {
        UUID orderId = UUID.randomUUID();
        OrderPaymentEventMessage message = new OrderPaymentEventMessage(orderId, null);

        ProcessOrderPaymentCommand command = message.toCommand();

        assertThat(command).isNotNull();
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.paymentStatus()).isEqualTo("CONFIRMED");
    }
}
