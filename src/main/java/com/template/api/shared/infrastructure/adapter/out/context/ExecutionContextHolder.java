package com.template.api.shared.infrastructure.adapter.out.context;

import com.template.api.shared.application.context.ExecutionContext;

/**
 * Thread-bound static carrier holding the active {@link ExecutionContext}.
 * <p>
 * Backed by a {@link ThreadLocal} with explicit cleanup semantics via {@link ThreadLocal#remove()},
 * preventing memory leaks and context contamination across thread pools and virtual thread workers.
 */
public final class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private ExecutionContextHolder() {}

    /**
     * Binds the execution context to the current thread. Clears the entry if {@code null}.
     *
     * @param context execution context to associate
     */
    public static void set(ExecutionContext context) {
        if (context == null) {
            clear();
        } else {
            CURRENT_CONTEXT.set(context);
        }
    }

    /**
     * Retrieves the execution context bound to the current thread without memory allocation.
     *
     * @return active execution context, or {@code null} if unset
     */
    public static ExecutionContext get() {
        return CURRENT_CONTEXT.get();
    }

    /**
     * Evicts the context entry from thread-local storage.
     */
    public static void clear() {
        CURRENT_CONTEXT.remove();
    }
}