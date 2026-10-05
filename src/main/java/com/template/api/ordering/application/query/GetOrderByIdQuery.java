package com.template.api.ordering.application.query;

import java.util.UUID;

/**
 * Immutable query to retrieve an order by its unique identifier.
 * <p>
 * Encapsulates parameters for read-only projection queries under the CQRS pattern.
 * Conforms to APP-02 and OUT-04.
 *
 * @param orderId target order identifier
 */
public record GetOrderByIdQuery(
        UUID orderId
) {
}
