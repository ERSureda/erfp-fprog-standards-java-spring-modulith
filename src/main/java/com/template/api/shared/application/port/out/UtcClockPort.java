package com.template.api.shared.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Outbound port abstracting temporal queries using the UTC standard.
 * <p>
 * Eliminates direct dependencies on static time providers (e.g., {@link Instant#now()}),
 * ensuring full determinism and reproducibility in unit tests and time-dependent business workflows.
 */
public interface UtcClockPort {

    Instant now();

    default LocalDate todayUtc() {
        return LocalDate.ofInstant(now(), ZoneOffset.UTC);
    }
}
