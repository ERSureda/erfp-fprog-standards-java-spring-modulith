package com.template.api.shared.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Outbound port abstracting temporal queries using the UTC standard.
 * <p>
 * Inverts the dependency on system clocks to guarantee reproducible deterministic testing.
 * Conforms to SED-01.
 */
public interface UtcClockPort {

    Instant now();

    default LocalDate todayUtc() {
        return LocalDate.ofInstant(now(), ZoneOffset.UTC);
    }
}
