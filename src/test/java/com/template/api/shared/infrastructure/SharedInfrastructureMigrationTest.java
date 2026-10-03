package com.template.api.shared.infrastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Shared Infrastructure Flyway Baseline Integration Test")
class SharedInfrastructureMigrationTest extends AbstractPostgresIntegrationTest {

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Should successfully apply V1 Flyway migration and verify baseline tables exist")
    void should_ApplyV1Migration_and_VerifyTablesExist() {
        assertThat(jdbcTemplate).isNotNull();

        // Verify outbox_events table exists
        Integer outboxCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'outbox_events'",
                Integer.class
        );
        assertThat(outboxCount).isEqualTo(1);

        // Verify processed_events table exists
        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'processed_events'",
                Integer.class
        );
        assertThat(processedCount).isEqualTo(1);
    }
}
