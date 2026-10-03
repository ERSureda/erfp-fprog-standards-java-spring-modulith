package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jdbc.query;

/**
 * SQL Query definitions for ordering JDBC projections.
 * Conforms to OUT-04 and OUT-05.
 */
public final class OrderJdbcQueries {

    private OrderJdbcQueries() {
        // Prevent instantiation of static utility class
    }

    public static final String SELECT_ORDER_BY_ID = """
        SELECT id, status, amount, currency
        FROM ordering.orders
        WHERE id = :id
        """;
}
