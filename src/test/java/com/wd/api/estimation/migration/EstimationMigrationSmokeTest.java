package com.wd.api.estimation.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EstimationMigrationSmokeTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("estimation_migration_smoke")
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
        // Re-enable Flyway (base class would disable it, but this test class doesn't extend the base).
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.table", () -> "portal_flyway_schema_history");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "1");
        // ddl-auto = validate ensures the entity definitions match what Flyway built.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Ancillary properties so context starts.
        registry.add("jwt.secret", () -> "test-secret-do-not-use-in-prod-0123456789abcdef0123456789abcdef");
        registry.add("jwt.access-token-expiration", () -> "3600000");
        registry.add("jwt.refresh-token-expiration", () -> "604800000");
        registry.add("storageBasePath", () -> "/tmp/test-storage-mig");
        registry.add("app.email.enabled", () -> "false");
        registry.add("app.rate-limiting.enabled", () -> "false");
    }

    @Autowired
    private DataSource dataSource;

    @Test
    void allMigrationsApplyCleanly_andSecondRunIsNoOp() {
        // Spring Boot already ran Flyway on context startup (because we set spring.flyway.enabled=true).
        // Calling migrate() a second time exercises idempotency.
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .table("portal_flyway_schema_history")
                .baselineOnMigrate(true)
                .baselineVersion("1")
                .load();

        MigrateResult result = flyway.migrate();
        assertThat(result.migrationsExecuted)
                .as("Second migrate() should be a no-op — all migrations already applied at context startup")
                .isZero();

        // Sanity: every migration we expect to have applied is in the applied list.
        long appliedCount = flyway.info().applied().length;
        assertThat(appliedCount)
                .as("Expected at least V88-V100 (13) plus the existing V1-V87 baseline migrations to be applied")
                .isGreaterThanOrEqualTo(100);
    }
}
