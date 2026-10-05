package com.template.api.shared.infrastructure.adapter.out.context;

import com.template.api.shared.application.context.ExecutionContext;

import java.util.UUID;

/**
 * Thread-bound static carrier holding the active {@link ExecutionContext}.
 * <p>
 * Backed by a {@link ThreadLocal} with explicit cleanup semantics via {@link ThreadLocal#remove()},
 * preventing thread pool memory leaks and context leakage across virtual threads.
 * Conforms to SED-03.
 */
public final class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> CURRENT_CONTEXT = new ThreadLocal<>();

    private ExecutionContextHolder() {}

    public static void set(ExecutionContext context) {
        if (context == null) {
            clear();
        } else {
            CURRENT_CONTEXT.set(context);
        }
    }

    public static ExecutionContext get() {
        return CURRENT_CONTEXT.get();
    }

    public static UUID getTenantId() {
        ExecutionContext ctx = get();
        return ctx != null ? ctx.tenantId() : null;
    }

    public static UUID getUserId() {
        ExecutionContext ctx = get();
        return ctx != null ? ctx.userId() : null;
    }

    public static void clear() {
        CURRENT_CONTEXT.remove();
    }
}
