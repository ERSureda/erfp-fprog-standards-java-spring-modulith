package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jdbc;

import com.template.api.ordering.application.port.out.OrderQueryPort;
import com.template.api.ordering.application.result.OrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter performing direct projection queries to OrderResult via NamedParameterJdbcTemplate.
 * Conforms to OUT-04 and OUT-05.
 */
@Repository
@RequiredArgsConstructor
public class OrderJdbcQueryAdapter implements OrderQueryPort {

    private static final String SELECT_ORDER_BY_ID_SQL = """
        SELECT id, status, amount, currency
        FROM ordering.orders
        WHERE id = :id
    """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Optional<OrderResult> findOrderResultById(UUID id) {
        return jdbcTemplate.query(SELECT_ORDER_BY_ID_SQL, Map.of("id", id), (rs, _) -> new OrderResult(
                rs.getObject("id", UUID.class),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getString("currency")
        )).stream().findFirst();
    }
}
