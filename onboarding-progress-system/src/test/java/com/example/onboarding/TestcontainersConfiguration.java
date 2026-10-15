package com.example.onboarding;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Starts a throwaway PostgreSQL (same version as docker-compose.yml) for tests.
 * Spring Boot points the app's datasource at it automatically via @ServiceConnection,
 * and Flyway creates the tables and sample data just like in production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:15");
    }
}
