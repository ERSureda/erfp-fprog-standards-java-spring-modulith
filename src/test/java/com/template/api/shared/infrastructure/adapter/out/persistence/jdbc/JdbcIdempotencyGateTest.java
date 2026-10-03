package com.template.api.shared.infrastructure.adapter.out.persistence.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JdbcIdempotencyGate Unit Tests")
class JdbcIdempotencyGateTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    private JdbcIdempotencyGate idempotencyGate;

    @BeforeEach
    void setUp() {
        idempotencyGate = new JdbcIdempotencyGate(jdbcTemplate);
    }

    @Test
    @DisplayName("tryAcquire should return true when insert succeeds")
    void tryAcquire_whenInsertSucceeds_shouldReturnTrue() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(contains("INSERT INTO processed_events"), anyMap())).thenReturn(1);

        boolean acquired = idempotencyGate.tryAcquire(eventId, "TestConsumer");

        assertThat(acquired).isTrue();
    }

    @Test
    @DisplayName("tryAcquire should return false on conflict duplicate")
    void tryAcquire_whenConflict_shouldReturnFalse() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(contains("INSERT INTO processed_events"), anyMap())).thenReturn(0);

        boolean acquired = idempotencyGate.tryAcquire(eventId, "TestConsumer");

        assertThat(acquired).isFalse();
    }

    @Test
    @DisplayName("release should execute delete and return true when row removed")
    void release_whenRowDeleted_shouldReturnTrue() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.update(contains("DELETE FROM processed_events"), anyMap())).thenReturn(1);

        boolean released = idempotencyGate.release(eventId);

        assertThat(released).isTrue();
        verify(jdbcTemplate).update(contains("DELETE FROM processed_events"), eq(Map.of("eventId", eventId)));
    }

    @Test
    @DisplayName("isProcessed should return true when count is positive")
    void isProcessed_whenCountPositive_shouldReturnTrue() {
        UUID eventId = UUID.randomUUID();
        when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(1);

        boolean processed = idempotencyGate.isProcessed(eventId);

        assertThat(processed).isTrue();
    }
}
