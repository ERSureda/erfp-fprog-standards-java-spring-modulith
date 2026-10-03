/**
 * Ordering Bounded Context.
 * <p>
 * Business module encapsulated as a {@link ApplicationModule.Type#CLOSED} Spring Modulith module.
 * Internal layers (domain and infrastructure) are strictly private to this module,
 * while the public API contract is explicitly exposed via {@code application}.
 */
@ApplicationModule(
        type = ApplicationModule.Type.CLOSED,
        displayName = "Ordering"
)
package com.template.api.ordering;

import org.springframework.modulith.ApplicationModule;
