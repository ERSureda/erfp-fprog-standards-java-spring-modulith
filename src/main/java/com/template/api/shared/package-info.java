/**
 * Shared Kernel Cross-Cutting Module.
 * <p>
 * Open transversal module declared as {@link org.springframework.modulith.ApplicationModule.Type#OPEN}
 * providing core Domain-Driven Design building blocks, execution context propagation, zero-overhead exceptions,
 * and common infrastructure adapters across all bounded contexts.
 */
@ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        displayName = "Shared"
)
package com.template.api.shared;

import org.springframework.modulith.ApplicationModule;