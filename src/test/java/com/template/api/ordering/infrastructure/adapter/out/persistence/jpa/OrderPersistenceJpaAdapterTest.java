package com.template.api.ordering.infrastructure.adapter.out.persistence.jpa;

import com.template.api.ordering.domain.model.Money;
import com.template.api.ordering.domain.model.Order;
import com.template.api.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderPersistenceJpaAdapter.class)
@DisplayName("OrderPersistenceJpaAdapter Integration Tests")
class OrderPersistenceJpaAdapterTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderPersistenceJpaAdapter adapter;

    @Test
    @DisplayName("should_PersistAndHydrateOrderCorrectly")
    void should_PersistAndHydrateOrderCorrectly() {
        // Arrange
        Order order = Order.create(UUID.randomUUID(), Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR")));

        // Act
        adapter.save(order);
        Optional<Order> loaded = adapter.findById(order.getId());

        // Assert
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(order.getId());
        assertThat(loaded.get().getVersion()).isEqualTo(0);
        assertThat(loaded.get().getAmount()).isEqualTo(order.getAmount());
    }
}
