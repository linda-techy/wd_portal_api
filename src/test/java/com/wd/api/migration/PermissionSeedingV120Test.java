package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V120__seed_baseline_permission.sql semantics.
 *
 * <p>Loads the SQL body from the classpath and exercises it directly
 * against the Hibernate-built test schema. Mirrors V117's INNER-JOIN
 * grant pattern: 1 permission row + 3 grants (ADMIN, PROJECT_MANAGER,
 * SCHEDULER); idempotent via ON CONFLICT DO NOTHING.
 */
@Transactional
class PermissionSeedingV120Test extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static String loadV120Sql() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("db/migration/V120__seed_baseline_permission.sql");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private void seedRoleFixture() {
        // Inline-seed a minimal portal_roles fixture so the JOIN has rows to bind.
        // The Hibernate-built schema already has portal_roles, portal_permissions,
        // and portal_role_permissions tables; V5 normally seeds the role rows AND
        // the UNIQUE(code) constraint via Flyway, but create-drop tests don't apply
        // Flyway. So we (a) defensively delete any pre-existing rows for the codes
        // we care about, then (b) plain INSERT (no ON CONFLICT — the column has no
        // unique constraint in the create-drop schema).
        jdbc.update("DELETE FROM portal_role_permissions");
        jdbc.update("DELETE FROM portal_permissions WHERE name = 'PROJECT_BASELINE_APPROVE'");
        jdbc.update("DELETE FROM portal_roles WHERE code IN ('ADMIN','PROJECT_MANAGER','SCHEDULER')");
        jdbc.update("INSERT INTO portal_roles(name, description, code) VALUES " +
                "('Admin','sys','ADMIN'), " +
                "('Project Manager','pm','PROJECT_MANAGER'), " +
                "('Scheduler','sched','SCHEDULER')");
    }

    @Test
    void v120_seedsPermissionAndGrantsToAdminPmScheduler() throws IOException {
        seedRoleFixture();

        // Run V120's SQL body.
        jdbc.execute(loadV120Sql());

        Integer permCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_permissions WHERE name = 'PROJECT_BASELINE_APPROVE'",
                Integer.class);
        assertThat(permCount).isEqualTo(1);

        Integer grantCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_role_permissions rp " +
                "JOIN portal_roles r ON r.id = rp.role_id " +
                "JOIN portal_permissions p ON p.id = rp.permission_id " +
                "WHERE p.name = 'PROJECT_BASELINE_APPROVE' " +
                "  AND r.code IN ('ADMIN','PROJECT_MANAGER','SCHEDULER')",
                Integer.class);
        assertThat(grantCount)
                .as("V120 must grant exactly 3 roles: ADMIN, PROJECT_MANAGER, SCHEDULER")
                .isEqualTo(3);
    }

    @Test
    void v120_isIdempotent_secondRunIsNoOp() throws IOException {
        seedRoleFixture();

        String sql = loadV120Sql();
        jdbc.execute(sql);
        jdbc.execute(sql);

        Integer permCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_permissions WHERE name = 'PROJECT_BASELINE_APPROVE'",
                Integer.class);
        assertThat(permCount).isEqualTo(1);

        Integer grantCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_role_permissions rp " +
                "JOIN portal_permissions p ON p.id = rp.permission_id " +
                "WHERE p.name = 'PROJECT_BASELINE_APPROVE'",
                Integer.class);
        assertThat(grantCount).isEqualTo(3);
    }
}
