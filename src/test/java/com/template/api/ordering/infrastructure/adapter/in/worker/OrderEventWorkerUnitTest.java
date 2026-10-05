package com.template.api.ordering.infrastructure.adapter.in.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.template.api.ordering.application.command.ProcessOrderPaymentCommand;
import com.template.api.ordering.application.port.in.ProcessOrderPaymentUseCase;
import com.template.api.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import com.template.api.shared.infrastructure.adapter.out.persistence.jdbc.JdbcIdempotencyGate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Isolated unit test suite for {@link OrderEventWorker}.
 * <p>
 * Verifies deserialization, idempotency gate interactions, context management, and defensive lock release on failure.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderEventWorker Unit Tests")
class OrderEventWorkerUnitTest {

    @Mock
    private ProcessOrderPaymentUseCase processOrderPaymentUseCase;

    @Mock
    private JdbcIdempotencyGate idempotencyGate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Executor directExecutor = Runnable::run;

    private OrderEventWorker worker;

    @BeforeEach
    void setUp() {
        worker = new OrderEventWorker(
                processOrderPaymentUseCase,
                idempotencyGate,
                objectMapper,
                directExecutor
        );
        ExecutionContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        ExecutionContextHolder.clear();
    }

    @Test
    @DisplayName("should_SkipProcessing_when_EventAlreadyProcessed")
    void should_SkipProcessing_when_EventAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyGate.tryAcquire(eventId, "OrderEventWorker")).thenReturn(false);

        worker.consumeOrderPaymentEvent(eventId, "{\"orderId\":\"" + UUID.randomUUID() + "\"}");

        verify(processOrderPaymentUseCase, never()).execute(any());
        assertThat(ExecutionContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("should_DeserializeAndExecuteUseCase_when_EventIsNew")
    void should_DeserializeAndExecuteUseCase_when_EventIsNew() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String json = """
            {
                "orderId": "%s",
                "paymentStatus": "CONFIRMED"
            }
        """.formatted(orderId);

        when(idempotencyGate.tryAcquire(eventId, "OrderEventWorker")).thenReturn(true);

        worker.consumeOrderPaymentEvent(eventId, json);

        ArgumentCaptor<ProcessOrderPaymentCommand> captor = ArgumentCaptor.forClass(ProcessOrderPaymentCommand.class);
        verify(processOrderPaymentUseCase).execute(captor.capture());

        ProcessOrderPaymentCommand command = captor.getValue();
        assertThat(command.orderId()).isEqualTo(orderId);
        assertThat(command.paymentStatus()).isEqualTo("CONFIRMED");
        assertThat(ExecutionContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("should_ReleaseIdempotencyAndThrow_when_DownstreamFails")
    void should_ReleaseIdempotencyAndThrow_when_DownstreamFails() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String json = """
            {
                "orderId": "%s",
                "paymentStatus": "CONFIRMED"
            }
        """.formatted(orderId);

        when(idempotencyGate.tryAcquire(eventId, "OrderEventWorker")).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("DB Connection Timeout"))
                .when(processOrderPaymentUseCase).execute(any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> worker.consumeOrderPaymentEvent(eventId, json).join())
                .isInstanceOf(java.util.concurrent.CompletionException.class)
                .hasCauseInstanceOf(com.template.api.shared.domain.exception.InfrastructureException.class);

        verify(idempotencyGate).release(eventId);
        assertThat(ExecutionContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("should_PropagateTenantId_when_PresentInPayload")
    void should_PropagateTenantId_when_PresentInPayload() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String json = """
            {
                "orderId": "%s",
                "paymentStatus": "CONFIRMED",
                "tenantId": "%s"
            }
        """.formatted(orderId, tenantId);

        when(idempotencyGate.tryAcquire(eventId, "OrderEventWorker")).thenReturn(true);

        worker.consumeOrderPaymentEvent(eventId, json);

        verify(processOrderPaymentUseCase).execute(any());
        assertThat(ExecutionContextHolder.get()).isNull();
    }
}
