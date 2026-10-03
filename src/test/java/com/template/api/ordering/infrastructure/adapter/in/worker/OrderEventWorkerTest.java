package com.template.api.ordering.infrastructure.adapter.in.worker;

import com.template.api.ordering.application.port.in.ProcessOrderPaymentUseCase;
import com.template.api.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DisplayName("OrderEventWorker Asynchronous & Idempotency Tests")
class OrderEventWorkerTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderEventWorker orderEventWorker;

    @Autowired(required = false)
    private NamedParameterJdbcTemplate jdbcTemplate;

    @MockitoBean
    private ProcessOrderPaymentUseCase processOrderPaymentUseCase;

    @BeforeEach
    void setUp() {
        if (jdbcTemplate != null) {
            jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE processed_events");
        }
    }

    @Test
    @DisplayName("should_DeduplicateEvent_when_ReceivedTwiceWithSameEventId")
    void should_DeduplicateEvent_when_ReceivedTwiceWithSameEventId() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String payloadJson = """
            {
                "orderId": "%s",
                "paymentStatus": "CONFIRMED"
            }
        """.formatted(UUID.randomUUID());

        // Act: Primera entrega
        orderEventWorker.consumeOrderPaymentEvent(eventId, payloadJson);

        // Assert: Esperar procesamiento asíncrono e inserción en tabla de idempotencia
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM processed_events WHERE event_id = :id",
                    Map.of("id", eventId),
                    Integer.class
            );
            assertThat(count).isEqualTo(1);
        });

        // Act: Replay del mismo evento (misma clave eventId)
        orderEventWorker.consumeOrderPaymentEvent(eventId, payloadJson);

        // Assert: Verificar deduplicación estricta (el caso de uso solo se invocó una vez)
        verify(processOrderPaymentUseCase, times(1)).execute(any());
    }
}
