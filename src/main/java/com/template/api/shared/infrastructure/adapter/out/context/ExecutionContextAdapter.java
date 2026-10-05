package com.template.api.shared.infrastructure.adapter.out.context;

import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.application.port.out.ExecutionContextPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Outbound adapter implementing {@link ExecutionContextPort} via {@link ExecutionContextHolder}.
 * <p>
 * Decouples use cases from thread-local static holders.
 * Conforms to SED-03.
 */
@Component
public class ExecutionContextAdapter implements ExecutionContextPort {

    @Override
    public ExecutionContext current() {
        ExecutionContext context = ExecutionContextHolder.get();
        if (context == null) {
            return ExecutionContext.anonymous();
        }
        return context;
    }

    @Override
    public Optional<ExecutionContext> findCurrent() {
        return Optional.ofNullable(ExecutionContextHolder.get());
    }
}
