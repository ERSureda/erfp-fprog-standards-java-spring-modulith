package com.template.api.ordering.infrastructure.adapter.in.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import com.template.api.ordering.application.port.in.ProcessOrderPaymentUseCase;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.infrastructure.adapter.out.persistence.jdbc.JdbcIdempotencyGate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Primary inbound asynchronous worker adapter consuming order payment events.
 * Implements an Idempotency Gate against processed_events (TRX-05).
 */
@Component
public class OrderEventWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderEventWorker.class);
    private static final String CONSUMER_NAME = "OrderEventWorker";

    private final ProcessOrderPaymentUseCase processOrderPaymentUseCase;
    private final JdbcIdempotencyGate idempotencyGate;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    @Autowired
    public OrderEventWorker(
            ProcessOrderPaymentUseCase processOrderPaymentUseCase,
            JdbcIdempotencyGate idempotencyGate,
            ObjectMapper objectMapper,
            @Autowired(required = false) Executor executor
    ) {
        this.processOrderPaymentUseCase = processOrderPaymentUseCase;
        this.idempotencyGate = idempotencyGate;
        this.objectMapper = objectMapper;
        this.executor = (executor != null) ? executor : Executors.newVirtualThreadPerTaskExecutor();
    }

    public OrderEventWorker(
            ProcessOrderPaymentUseCase processOrderPaymentUseCase,
            JdbcIdempotencyGate idempotencyGate,
            ObjectMapper objectMapper
    ) {
        this(processOrderPaymentUseCase, idempotencyGate, objectMapper, null);
    }

    /**
     * Consumes and processes an order payment event idempotently.
     */
    public void consumeOrderPaymentEvent(UUID eventId, String payloadJson) {
        CompletableFuture.runAsync(() -> {
            if (!idempotencyGate.tryAcquire(eventId, CONSUMER_NAME)) {
                log.info("Event [{}] already processed by [{}]. Skipping duplicate.", eventId, CONSUMER_NAME);
                return;
            }

            try {
                JsonNode root = objectMapper.readTree(payloadJson);
                UUID orderId = UUID.fromString(root.path("orderId").asText());
                String paymentStatus = root.path("paymentStatus").asText("CONFIRMED");

                ProcessOrderPaymentCommand command = new ProcessOrderPaymentCommand(orderId, paymentStatus);
                processOrderPaymentUseCase.execute(command);
            } catch (Exception ex) {
                log.error("Failed to process order payment event [{}]", eventId, ex);
                throw new InfrastructureException("Error processing event " + eventId, ex);
            }
        }, executor);
    }
}
