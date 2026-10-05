package com.template.api.shared.application.port.out;

import com.template.api.shared.application.context.ExecutionContext;

import java.util.Optional;

/**
 * Outbound port providing access to the current request execution context.
 * <p>
 * Decouples application services from static infrastructure thread holders.
 * Conforms to SED-03.
 */
public interface ExecutionContextPort {

    ExecutionContext current();

    Optional<ExecutionContext> findCurrent();
}
