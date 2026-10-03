/**
 * Shared Kernel Cross-Cutting Module.
 * <p>
 * Open transversal module ({@link ApplicationModule.Type#OPEN}) providing core DDD building blocks,
 * execution context, zero-overhead exceptions, and common infrastructure adapters to all bounded contexts.
 */
@ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        displayName = "Shared"
)
package com.template.api.shared;

import org.springframework.modulith.ApplicationModule;