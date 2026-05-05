package com.wd.api.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for tests that need a real Postgres for migration regression
 * testing.
 *
 * <p>Note: this codebase's V1__baseline_schema.sql is documentation-only —
 * the V1-era schema is bootstrapped by Hibernate from @Entity definitions,
 * THEN Flyway runs V2+ on top. Running Flyway against a truly empty Postgres
 * fails at V2 (it expects tables that V1 was supposed to have created).
 *
 * <p>For test purposes we therefore mirror the production "first boot"
 * behaviour: Hibernate creates the full schema from entities (which already
 * includes the JPA mappings of the new V112+ tables), and the migration test
 * exercises the V113 SQL body directly via JdbcTemplate — that's the
 * behaviour the SQL itself implements, and idempotency is preserved.
 */
@SpringBootTest
public abstract class FlywayMigrationTestBase {

    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("portalapi_migration_test")
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
        // Mirror TestcontainersPostgresBase: Hibernate owns schema, Flyway off.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("jwt.secret", () -> "test-secret-do-not-use-in-prod-0123456789abcdef0123456789abcdef");
        registry.add("jwt.access-token-expiration", () -> "3600000");
        registry.add("jwt.refresh-token-expiration", () -> "604800000");
        registry.add("storageBasePath", () -> "/tmp/test-storage");
        registry.add("app.email.enabled", () -> "false");
        registry.add("app.rate-limiting.enabled", () -> "false");
        registry.add("spring.jackson.serialization.fail-on-empty-beans", () -> "false");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations", () -> "classpath:schema-postgres.sql");
        registry.add("spring.jpa.defer-datasource-initialization", () -> "true");
    }
}
