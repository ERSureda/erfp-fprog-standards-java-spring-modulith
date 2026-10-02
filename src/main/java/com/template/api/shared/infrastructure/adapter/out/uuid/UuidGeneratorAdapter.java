package com.template.api.shared.infrastructure.adapter.out.uuid;

import com.template.api.shared.application.port.out.UuidGeneratorPort;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance UUIDv7 generator compliant with RFC 9562.
 * <p>
 * Combines a 48-bit Unix millisecond epoch timestamp with an integrated 12-bit monotonic sub-millisecond
 * sequence counter inside a single atomic state register. Enforces non-blocking thread-safety via lock-free
 * CAS semantics and borrows from subsequent milliseconds on counter exhaustion to maintain chronological ordering.
 * Generates sequentially ordered identifiers to prevent PostgreSQL B-Tree index page splitting.
 */
@Component
public final class UuidGeneratorAdapter implements UuidGeneratorPort {

    private static final int COUNTER_BITS = 12;
    private static final long COUNTER_MASK = (1L << COUNTER_BITS) - 1;
    private static final long TIMESTAMP_MASK = (1L << 48) - 1;
    private static final int TIMESTAMP_SHIFT = 16;
    private static final long VERSION_7 = 0x7000L;
    private static final long VARIANT_RFC = 0x8000000000000000L;
    private static final long RANDOM_MASK = 0x3FFFFFFFFFFFFFFFL;

    private final AtomicLong state = new AtomicLong();

    @Override
    public UUID generateId() {
        long claimed = claim();
        long timestamp = claimed >>> COUNTER_BITS;
        long counter = claimed & COUNTER_MASK;

        long mostSignificantBits = ((timestamp & TIMESTAMP_MASK) << TIMESTAMP_SHIFT) | VERSION_7 | counter;
        long leastSignificantBits = (ThreadLocalRandom.current().nextLong() & RANDOM_MASK) | VARIANT_RFC;

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    private long claim() {
        long current;
        long next;
        do {
            current = state.get();
            long now = System.currentTimeMillis();
            next = now > (current >>> COUNTER_BITS) ? (now << COUNTER_BITS) : (current + 1);
        } while (!state.compareAndSet(current, next));

        return next;
    }
}