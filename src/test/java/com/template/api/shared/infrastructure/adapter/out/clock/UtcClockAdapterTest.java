package com.template.api.shared.infrastructure.adapter.out.clock;

import com.template.api.shared.application.port.out.UtcClockPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UtcClockAdapter Unit Tests")
class UtcClockAdapterTest {

    @Test
    @DisplayName("Should implement UtcClockPort and default to system UTC when clock is null")
    void nullClock_shouldDefaultToSystemUtc() {
        UtcClockAdapter clockAdapter = new UtcClockAdapter(null);

        assertThat(clockAdapter).isInstanceOf(UtcClockPort.class);

        Instant before = Instant.now();
        Instant clockInstant = clockAdapter.now();
        Instant after = Instant.now();

        assertThat(clockInstant).isNotNull();
        assertThat(clockInstant).isBetween(before.minus(Duration.ofMillis(100)), after.plus(Duration.ofMillis(100)));
    }

    @Test
    @DisplayName("Should return fixed instant when configured with fixed clock")
    void customConstructor_withFixedClock_shouldReturnFixedInstant() {
        Instant fixedInstant = Instant.parse("2026-09-12T10:15:30.00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        UtcClockAdapter clockAdapter = new UtcClockAdapter(fixedClock);

        assertThat(clockAdapter.now()).isEqualTo(fixedInstant);
    }

    @Test
    @DisplayName("Should correctly calculate todayUtc via default method")
    void todayUtc_shouldReturnCurrentDateInUtc() {
        Instant fixedInstant = Instant.parse("2026-09-12T23:59:59.00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        UtcClockAdapter clockAdapter = new UtcClockAdapter(fixedClock);

        assertThat(clockAdapter.todayUtc()).isEqualTo(LocalDate.of(2026, 9, 12));
    }
}