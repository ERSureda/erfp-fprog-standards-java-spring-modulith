package com.template.api.ordering.application.query;

import java.util.UUID;

/**
 * Immutable query to retrieve an order by its identifier.
 */
public record GetOrderByIdQuery(
        UUID orderId
) {
}
