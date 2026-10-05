package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jdbc.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test suite for {@link OrderJdbcQueries}.
 * <p>
 * Verifies static SQL text block definitions for query syntax and parameter binding presence.
 */
@DisplayName("OrderJdbcQueries Unit Tests")
class OrderJdbcQueriesTest {

    @Test
    @DisplayName("should_DefineValidSelectOrderByIdQuery")
    void should_DefineValidSelectOrderByIdQuery() {
        String query = OrderJdbcQueries.SELECT_ORDER_BY_ID;

        assertThat(query)
                .isNotBlank()
                .contains("SELECT id, status, amount, currency")
                .contains("FROM ordering.orders")
                .contains("WHERE id = :id");
    }
}
