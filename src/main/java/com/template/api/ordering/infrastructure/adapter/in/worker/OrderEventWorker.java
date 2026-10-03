package com.template.api.ordering.infrastructure.adapter.in.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.ordering.application.port.in.ProcessOrderPaymentUseCase;
import com.template.api.ordering.infrastructure.adapter.in.worker.dto.OrderPaymentEventMessage;
import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.domain.exception.InfrastructureException;
import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import com.template.api.shared.infrastructure.adapter.out.persistence.jdbc.JdbcIdempotencyGate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Primary inbound asynchronous worker adapter consuming order payment events.
 * Implements an Idempotency Gate against processed_events (TRX-05).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventWorker {

    private static final String CONSUMER_NAME = "OrderEventWorker";

    private final ProcessOrderPaymentUseCase processOrderPaymentUseCase;
    private final JdbcIdempotencyGate idempotencyGate;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    /**
     * Consumes and processes an order payment event idempotently.
     *
     * @param eventId     unique identifier of the incoming event
     * @param payloadJson raw JSON message payload
     * @return a {@link CompletableFuture} completing when processing and persistence conclude
     */
    public CompletableFuture<Void> consumeOrderPaymentEvent(UUID eventId, String payloadJson) {
        return CompletableFuture.runAsync(() -> {
            if (!idempotencyGate.tryAcquire(eventId, CONSUMER_NAME)) {
                log.info("Event [{}] already processed by [{}]. Skipping duplicate.", eventId, CONSUMER_NAME);
                return;
            }

            try {
                // Direct streaming token deserialization to immutable record (zero DOM allocation)
                OrderPaymentEventMessage message = objectMapper.readValue(payloadJson, OrderPaymentEventMessage.class);

                // INP-04: Initialize worker context with system actor, tenant (if present), and correlationId
                ExecutionContextHolder.set(new ExecutionContext(message.tenantId(), null, Set.of("SYSTEM"), eventId.toString()));

                processOrderPaymentUseCase.execute(message.toCommand());
            } catch (Exception ex) {
                // Release idempotency reservation on failure so subsequent message retries can be processed
                idempotencyGate.release(eventId);
                log.error("Failed to process order payment event [{}]", eventId, ex);
                throw new InfrastructureException("Error processing event " + eventId, ex);
            } finally {
                ExecutionContextHolder.clear();
            }
        }, executor);
    }
}
