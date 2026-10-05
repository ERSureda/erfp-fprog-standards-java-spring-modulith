package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.repository;

import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for Order persistence entities.
 * <p>
 * Confined to the internal persistence infrastructure package.
 * Conforms to OUT-01.
 */
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {
}
