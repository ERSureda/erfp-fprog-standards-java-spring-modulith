package com.template.api.shared.application.port.out;

import com.template.api.shared.application.context.ExecutionContext;

import java.util.Optional;

/**
 * Outbound port providing access to the current request execution context.
 * <p>
 * Inverts the dependency between the application layer and runtime infrastructure
 * (such as HTTP filters, ThreadLocal state, or reactive contexts). It enables use cases
 * to retrieve identity, tenant, and role metadata without importing static infrastructure holders.
 */
public interface ExecutionContextPort {

    ExecutionContext current();
    Optional<ExecutionContext> findCurrent();
}
