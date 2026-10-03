package com.template.api.ordering.infrastructure.adapter.in.web;

import com.template.api.ordering.application.command.CreateOrderCommand;
import com.template.api.ordering.application.port.in.CreateOrderUseCase;
import com.template.api.ordering.application.port.in.GetOrderByIdUseCase;
import com.template.api.ordering.application.query.GetOrderByIdQuery;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.infrastructure.adapter.in.web.request.CreateOrderRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Primary inbound HTTP adapter for orders.
 * Conforms to INP-01, INP-02, and ADR-005.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;

    @Autowired
    public OrderController(
            CreateOrderUseCase createOrderUseCase,
            @Autowired(required = false) GetOrderByIdUseCase getOrderByIdUseCase
    ) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderByIdUseCase = getOrderByIdUseCase;
    }

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this(createOrderUseCase, null);
    }

    @PostMapping
    public ResponseEntity<OrderResult> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderCommand command = new CreateOrderCommand(request.amount(), request.currency());
        OrderResult result = createOrderUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResult> getOrderById(@PathVariable UUID id) {
        if (getOrderByIdUseCase == null) {
            return ResponseEntity.notFound().build();
        }
        OrderResult result = getOrderByIdUseCase.execute(new GetOrderByIdQuery(id));
        return ResponseEntity.ok(result);
    }
}
