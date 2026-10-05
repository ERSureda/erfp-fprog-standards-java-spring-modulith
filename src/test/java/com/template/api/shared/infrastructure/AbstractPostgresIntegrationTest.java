package com.template.api.shared.infrastructure;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared singleton PostgreSQL 17 Testcontainers base class for integration tests.
 * <p>
 * Reuses a single PostgreSQL 17 container instance across all integration tests in the test suite
 * to fulfill CI execution time budgets without restarting containers per test class (TESTING.md Section 6.1).
 * Enforces real PostgreSQL 17 persistence, eliminating in-memory database divergence (TST-03).
 */
public abstract class AbstractPostgresIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractPostgresIntegrationTest.class);
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;
    private static final boolean DOCKER_AVAILABLE;

    static {
        boolean available = false;
        PostgreSQLContainer<?> container = null;
        try {
            available = DockerClientFactory.instance().isDockerAvailable();
            if (available) {
                container = new PostgreSQLContainer<>("postgres:17-alpine")
                        .withDatabaseName("test_db")
                        .withUsername("test_user")
                        .withPassword("test_pass")
                        .withReuse(true);
                container.start();
                log.info("PostgreSQL 17 Testcontainers singleton started at: {}", container.getJdbcUrl());
            } else {
                log.warn("Docker environment not detected. Integration tests extending AbstractPostgresIntegrationTest will be skipped.");
            }
        } catch (Exception ex) {
            log.warn("Failed to initialize PostgreSQL Testcontainer: {}. Tests will be skipped.", ex.getMessage());
            available = false;
            container = null;
        }
        DOCKER_AVAILABLE = available;
        POSTGRES_CONTAINER = container;
    }

    @BeforeAll
    static void ensureDockerRunning() {
        Assumptions.assumeTrue(
                DOCKER_AVAILABLE && POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning(),
                "Skipping test: Docker environment is not available or PostgreSQL container is not running."
        );
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES_CONTAINER != null && POSTGRES_CONTAINER.isRunning()) {
            registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
            registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.flyway.enabled", () -> "true");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        }
    }
}
