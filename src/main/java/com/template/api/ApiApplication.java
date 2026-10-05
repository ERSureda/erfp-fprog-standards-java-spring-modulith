package com.template.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for the Modular Monolith API.
 * <p>
 * Bootstraps the Spring Boot runtime environment, initializes the Spring Modulith
 * container, and configures cross-cutting infrastructure beans.
 */
@SpringBootApplication
public class ApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiApplication.class, args);
    }
}
