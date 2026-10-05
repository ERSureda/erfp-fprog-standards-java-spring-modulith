package com.template.api;

import com.template.api.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Test suite verifying bootstrap and configuration of the Spring application context.
 * <p>
 * Ensures all components, configuration properties, and database connections initialize cleanly.
 */
@SpringBootTest
@DisplayName("Application Context Tests")
class ApiApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    @DisplayName("Should successfully load the Spring application context")
    void contextLoads() {
    }
}
