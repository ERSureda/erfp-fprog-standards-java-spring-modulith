package com.template.api.shared.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying Flyway database schema migrations for shared infrastructure tables.
 * <p>
 * Verifies that the initial baseline migrations execute successfully against a real PostgreSQL instance
 * and instantiate requisite infrastructure tables such as outbox and idempotency persistence stores (TST-03).
 */
@SpringBootTest
@DisplayName("Shared Infrastructure Flyway Baseline Integration Test")
class SharedInfrastructureMigrationTest extends AbstractPostgresIntegrationTest {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should successfully apply V1 Flyway migration and verify baseline tables exist")
    void should_ApplyV1Migration_and_VerifyTablesExist() {
        assertThat(jdbcTemplate).isNotNull();

        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'outbox_events'",
                Integer.class
        );
        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'processed_events'",
                Integer.class
        );

        assertThat(outboxCount).isEqualTo(1);
        assertThat(processedCount).isEqualTo(1);
    }
}
