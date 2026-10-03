package com.template.api.ordering.infrastructure.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for Order entities.
 */
public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID> {
}
