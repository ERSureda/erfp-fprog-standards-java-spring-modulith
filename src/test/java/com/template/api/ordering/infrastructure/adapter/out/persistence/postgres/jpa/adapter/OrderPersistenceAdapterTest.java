package com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.adapter;

import com.template.api.ordering.domain.model.Money;
import com.template.api.ordering.domain.model.Order;
import com.template.api.ordering.infrastructure.adapter.out.persistence.postgres.jpa.mapper.OrderPersistenceMapper;
import com.template.api.shared.application.port.out.OutboxPublisherPort;
import com.template.api.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderPersistenceAdapter.class)
@ComponentScan(basePackageClasses = OrderPersistenceMapper.class)
@DisplayName("OrderPersistenceAdapter Integration Tests")
class OrderPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderPersistenceAdapter adapter;

    @MockitoBean
    private OutboxPublisherPort outboxPublisherPort;

    @Test
    @DisplayName("should_PersistAndHydrateOrderCorrectly_and_PublishDomainEventsToOutbox")
    void should_PersistAndHydrateOrderCorrectly_and_PublishDomainEventsToOutbox() {
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
        verify(outboxPublisherPort).publishAll(anyList());
    }
}
