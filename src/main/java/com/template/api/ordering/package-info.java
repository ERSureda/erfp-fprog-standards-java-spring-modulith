/**
 * Ordering Bounded Context.
 * <p>
 * Encapsulated business module declared as a {@link org.springframework.modulith.ApplicationModule.Type#CLOSED}
 * Spring Modulith container. Internal layers (domain and infrastructure) are strictly package-private,
 * exposing capabilities exclusively through the {@code application} named interface.
 */
@ApplicationModule(
        type = ApplicationModule.Type.CLOSED,
        displayName = "Ordering"
)
package com.template.api.ordering;

import org.springframework.modulith.ApplicationModule;
