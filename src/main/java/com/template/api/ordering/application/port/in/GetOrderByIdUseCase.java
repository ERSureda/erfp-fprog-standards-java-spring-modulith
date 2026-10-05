package com.template.api.ordering.application.port.in;

import com.template.api.ordering.application.query.GetOrderByIdQuery;
import com.template.api.ordering.application.result.OrderResult;

/**
 * Primary inbound port defining the contract for order retrieval by identifier.
 * <p>
 * Implemented by application services to execute CQRS read-only queries directly returning DTO projections.
 * Conforms to APP-01 and OUT-04.
 */
public interface GetOrderByIdUseCase {

    OrderResult execute(GetOrderByIdQuery query);
}
