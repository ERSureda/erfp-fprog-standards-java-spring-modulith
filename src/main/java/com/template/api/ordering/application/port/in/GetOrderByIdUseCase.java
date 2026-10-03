package com.template.api.ordering.application.port.in;

import com.template.api.ordering.application.query.GetOrderByIdQuery;
import com.template.api.ordering.application.result.OrderResult;

/**
 * Primary port / Use Case for retrieving order details by ID (CQRS Query).
 */
public interface GetOrderByIdUseCase {
    OrderResult execute(GetOrderByIdQuery query);
}
