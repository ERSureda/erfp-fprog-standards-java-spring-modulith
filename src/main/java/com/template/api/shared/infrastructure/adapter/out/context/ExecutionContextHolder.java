package com.template.api.shared.infrastructure.adapter.out.context;

import com.template.api.shared.application.context.ExecutionContext;

import java.util.UUID;

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
     * Returns the tenant ID of the current context, or {@code null} if no context or tenant is present.
     */
    public static UUID getTenantId() {
        ExecutionContext ctx = get();
        return ctx != null ? ctx.tenantId() : null;
    }

    /**
     * Returns the user ID of the current context, or {@code null} if no context or user is present.
     */
    public static UUID getUserId() {
        ExecutionContext ctx = get();
        return ctx != null ? ctx.userId() : null;
    }

    /**
     * Evicts the context entry from thread-local storage.
     */
    public static void clear() {
        CURRENT_CONTEXT.remove();
    }
}