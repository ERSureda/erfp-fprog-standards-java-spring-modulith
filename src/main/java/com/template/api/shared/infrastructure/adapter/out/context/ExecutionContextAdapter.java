package com.template.api.shared.infrastructure.adapter.out.context;

import com.template.api.shared.application.context.ExecutionContext;
import com.template.api.shared.application.port.out.ExecutionContextPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Outbound adapter providing application use cases access to the current request execution context.
 * Inverts dependency on {@link ExecutionContextHolder} thread-local storage.
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
