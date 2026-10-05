package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jdbc.adapter;

import com.template.api.ordering.application.port.out.OrderQueryPort;
import com.template.api.ordering.application.result.OrderResult;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jdbc.repository.OrderJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Secondary outbound persistence adapter implementing {@link OrderQueryPort} via Spring JDBC.
 * <p>
 * Executes lightweight SQL projection queries returning DTOs directly without ORM overhead.
 * Conforms to OUT-04 and OUT-05.
 */
@Component
@RequiredArgsConstructor
public class OrderJdbcQueryAdapter implements OrderQueryPort {

    private final OrderJdbcRepository repository;

    @Override
    public Optional<OrderResult> findOrderResultById(UUID id) {
        return repository.findOrderResultById(id);
    }
}
