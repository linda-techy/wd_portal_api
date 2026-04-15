package com.wd.api.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Ensures that if a previous Flyway migration failed (e.g., due to a syntax error
 * or missing table), the failed state is automatically "repaired" (cleared) on startup 
 * before attempting the migration again. This prevents the application from being permanently
 * locked out of booting after a bad migration.
 */
@Configuration
@Profile({"local", "staging", "production"})
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Repair any failed migrations (removes failed rows from flyway_schema_history)
            flyway.repair();
            
            // Then proceed with the normal migration
            flyway.migrate();
        };
    }
}
