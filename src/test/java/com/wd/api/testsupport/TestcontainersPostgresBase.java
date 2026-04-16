package com.wd.api.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base class for portal-api integration tests that need a real Postgres.
 * Starts a Postgres 16 container once per JVM via static initializer.
 * Ryuk sidecar tears it down at JVM exit.
 */
@SpringBootTest
public abstract class TestcontainersPostgresBase {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("portalapi_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Use create-drop for tests: Hibernate generates the full schema from entity classes.
        // V1__baseline_schema.sql is "documentation-only" and contains no DDL, so Flyway is disabled
        // to avoid V1 baseline complexity. For production, Flyway manages migrations; tests just need
        // the schema and data that entities define (no need for V2+ migrations in test context).
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-do-not-use-in-prod-0123456789abcdef0123456789abcdef");
        registry.add("jwt.access-token-expiration", () -> "3600000");
        registry.add("jwt.refresh-token-expiration", () -> "604800000");
        // storageBasePath: required by BrochureController and FileDownloadController.
        // Use /tmp/test-storage for tests — it's ephemeral and isolated per run.
        registry.add("storageBasePath", () -> "/tmp/test-storage");
        // app.email.enabled: disable email sending in tests to avoid mail server dependency.
        registry.add("app.email.enabled", () -> "false");
    }
}
