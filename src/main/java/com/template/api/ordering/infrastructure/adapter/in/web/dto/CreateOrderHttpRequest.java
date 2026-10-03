package com.template.api.ordering.infrastructure.adapter.in.web.dto;

import com.template.api.ordering.application.command.CreateOrderCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Web HTTP request payload with Jakarta validation constraints for creating orders.
 * Exposes a zero-overhead factory method to map directly to the application command.
 */
public record CreateOrderHttpRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO code")
        String currency
) {

    /**
     * Converts this validated web request into an immutable application command.
     *
     * @return an immutable {@link CreateOrderCommand}
     */
    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(this.amount, this.currency);
    }
}
