package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies V125__add_pending_pm_approval_status.sql semantics. Loads the SQL
 * body from the classpath and runs it against a Hibernate-built test schema.
 * Mirrors V120 / V122 pattern.
 *
 * <p>Seeds ADMIN + PROJECT_MANAGER role rows so the inner-join grant section
 * has rows to bind to (matches PermissionSeedingV120Test convention).
 */
class V125PendingPmApprovalStatusTest extends FlywayMigrationTestBase {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        // DDL is auto-committed; restore expected pre-V125 shape for siblings.
        jdbc.execute("ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_chk");
        jdbc.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS rejection_reason");
        jdbc.update("DELETE FROM portal_role_permissions rp USING portal_permissions p " +
                "WHERE rp.permission_id = p.id AND p.name = 'TASK_COMPLETION_APPROVE'");
        jdbc.update("DELETE FROM portal_permissions WHERE name = 'TASK_COMPLETION_APPROVE'");
    }

    private static String loadSql() throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V125__add_pending_pm_approval_status.sql")
                        .getInputStream(),
                StandardCharsets.UTF_8);
    }

    private void seedAdminPmRoles() {
        // Mirrors PermissionSeedingV120Test.seedRoleFixture: clean slate first
        // so we don't double-seed across re-runs in the same container.
        jdbc.execute(
                "DELETE FROM portal_role_permissions rp USING portal_permissions p " +
                "WHERE rp.permission_id = p.id AND p.name = 'TASK_COMPLETION_APPROVE'");
        jdbc.update("DELETE FROM portal_permissions WHERE name = 'TASK_COMPLETION_APPROVE'");
        jdbc.update("DELETE FROM portal_roles WHERE code IN ('ADMIN','PROJECT_MANAGER')");
        jdbc.update("INSERT INTO portal_roles(name, description, code) VALUES " +
                "('Admin','sys','ADMIN'), ('Project Manager','pm','PROJECT_MANAGER')");
    }

    private long insertProject(long id) {
        jdbc.update(
                "INSERT INTO customer_projects (id, project_uuid, name, location, " +
                "is_design_agreement_signed, created_at, updated_at, version) " +
                "VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1) " +
                "ON CONFLICT (id) DO NOTHING",
                id, UUID.randomUUID(), "v125-test-" + id, "Test Location");
        return id;
    }

    /**
     * Drops Hibernate's auto-generated {@code tasks_status_check} constraint
     * (which mirrors the Java {@link com.wd.api.model.Task.TaskStatus} values
     * known at boot time). Production schemas don't have this constraint
     * (Flyway-managed), so removing it before V125 runs is the faithful
     * simulation of the pre-V125 state. V125's named {@code tasks_status_chk}
     * then becomes the sole CHECK on the column.
     */
    private void dropHibernateAutoStatusCheck() {
        jdbc.execute("ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_status_check");
    }

    @Test
    void v125_checkConstraintAdmitsPendingPmApproval() throws IOException {
        seedAdminPmRoles();
        dropHibernateAutoStatusCheck();
        jdbc.execute(loadSql());
        long projectId = insertProject(91250L);

        // Should NOT throw — PENDING_PM_APPROVAL is now in the CHECK list.
        jdbc.update(
                "INSERT INTO tasks (title, status, priority, project_id, due_date, " +
                "customer_visible, monsoon_sensitive, created_at, updated_at, version) " +
                "VALUES (?, ?, ?, ?, CURRENT_DATE, true, false, NOW(), NOW(), 1)",
                "t", "PENDING_PM_APPROVAL", "MEDIUM", projectId);
    }

    @Test
    void v125_checkConstraintRejectsUnknownStatus() throws IOException {
        seedAdminPmRoles();
        dropHibernateAutoStatusCheck();
        jdbc.execute(loadSql());
        long projectId = insertProject(91251L);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO tasks (title, status, priority, project_id, due_date, " +
                "customer_visible, monsoon_sensitive, created_at, updated_at, version) " +
                "VALUES (?, ?, ?, ?, CURRENT_DATE, true, false, NOW(), NOW(), 1)",
                "t", "TOTALLY_BOGUS", "MEDIUM", projectId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v125_addsRejectionReasonColumn() throws IOException {
        seedAdminPmRoles();
        jdbc.execute(loadSql());
        Integer charLen = jdbc.queryForObject(
                "SELECT character_maximum_length FROM information_schema.columns " +
                "WHERE table_name = 'tasks' AND column_name = 'rejection_reason'",
                Integer.class);
        assertThat(charLen).isEqualTo(500);
    }

    @Test
    void v125_seedsPermissionAndGrantsAdminAndPm() throws IOException {
        seedAdminPmRoles();
        jdbc.execute(loadSql());

        Integer permCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_permissions WHERE name = 'TASK_COMPLETION_APPROVE'",
                Integer.class);
        assertThat(permCount).isEqualTo(1);

        Integer grantCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_role_permissions rp " +
                "JOIN portal_roles r ON r.id = rp.role_id " +
                "JOIN portal_permissions p ON p.id = rp.permission_id " +
                "WHERE p.name = 'TASK_COMPLETION_APPROVE' AND r.code IN ('ADMIN','PROJECT_MANAGER')",
                Integer.class);
        assertThat(grantCount)
                .as("V125 must grant exactly 2 roles: ADMIN, PROJECT_MANAGER")
                .isEqualTo(2);
    }

    @Test
    void v125_isIdempotent() throws IOException {
        seedAdminPmRoles();
        String sql = loadSql();
        jdbc.execute(sql);
        jdbc.execute(sql);
    }
}
