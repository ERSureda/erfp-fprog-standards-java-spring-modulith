package com.template.api.ordering.infrastructure.adapter.in.web.dto;

import com.template.api.ordering.application.command.CreateOrderCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * HTTP request payload with Bean Validation constraints for order creation.
 * <p>
 * Decouples public web schema definitions from internal application command structures.
 * Conforms to INP-01 and ADR-005.
 *
 * @param amount   monetary total of the order, must be greater than zero
 * @param currency three-letter ISO-4217 currency code
 */
public record CreateOrderHttpRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency
) {

    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(this.amount, this.currency);
    }
}
