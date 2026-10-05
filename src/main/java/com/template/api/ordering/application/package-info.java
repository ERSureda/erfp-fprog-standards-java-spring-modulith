/**
 * Public Application API for the Ordering Bounded Context.
 * <p>
 * Declared as a {@link org.springframework.modulith.NamedInterface} to serve as the sole gateway through which
 * external Modulith modules interact with Ordering via inbound use case contracts, commands, queries, and DTOs.
 */
@NamedInterface("application")
package com.template.api.ordering.application;

import org.springframework.modulith.NamedInterface;
