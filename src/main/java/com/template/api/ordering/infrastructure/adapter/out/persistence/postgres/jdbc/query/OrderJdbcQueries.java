package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jdbc.query;

/**
 * SQL query definitions for Ordering JDBC projections.
 * <p>
 * Maintains centralized SQL text blocks for optimized projection queries.
 * Conforms to OUT-04 and OUT-05.
 */
public final class OrderJdbcQueries {

    private OrderJdbcQueries() {}

    public static final String SELECT_ORDER_BY_ID = """
        SELECT id, status, amount, currency
        FROM ordering.orders
        WHERE id = :id
        """;
}
