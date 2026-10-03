package com.template.api;

import com.template.api.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("Application Context Tests")
class ApiApplicationTests extends AbstractPostgresIntegrationTest {

    @Test
    @DisplayName("Should successfully load the Spring application context")
    void contextLoads() {
    }
}
