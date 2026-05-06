package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V121__drop_task_depends_on_task_id.sql semantics.
 *
 * <p>Loads V121's SQL body from the classpath and exercises it directly
 * against the Hibernate-built test schema.
 *
 * <p>Note: post-Task 10 the {@code dependsOnTaskId} field is removed
 * from the JPA Task entity, so Hibernate's create-drop schema does not
 * have the column. To faithfully test V121 we simulate the pre-drop
 * production state by ALTER TABLE ADDing the column first, then run
 * V121 and confirm it is gone. The {@code IF EXISTS} clause makes V121
 * a safe no-op when the column is already absent.
 */
class V121DropDependsOnTaskIdTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static String loadV121Sql() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("db/migration/V121__drop_task_depends_on_task_id.sql");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    void v121_dropsLegacyColumn_butLeavesPredecessorTableIntact() throws Exception {
        // Simulate pre-V121 production state by adding the legacy column.
        jdbc.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS depends_on_task_id BIGINT");

        long projectId = 9999L;
        jdbc.update("INSERT INTO customer_projects (id, project_uuid, name, location, " +
                    "is_design_agreement_signed, created_at, updated_at, version) " +
                    "VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1) " +
                    "ON CONFLICT (id) DO NOTHING",
                projectId, UUID.randomUUID(), "v121-test-project", "Test");

        jdbc.update("DELETE FROM task_predecessor WHERE successor_id = 9002 AND predecessor_id = 9001");
        jdbc.update("DELETE FROM tasks WHERE id IN (9001, 9002)");
        jdbc.update("INSERT INTO tasks(id, title, status, priority, due_date, project_id, " +
                    "customer_visible, monsoon_sensitive, created_at, updated_at, version, " +
                    "depends_on_task_id) VALUES " +
                    "(9001, 'Foundation', 'PENDING', 'MEDIUM', '2026-06-30', ?, true, false, " +
                    "NOW(), NOW(), 1, NULL), " +
                    "(9002, 'Slab',       'PENDING', 'MEDIUM', '2026-06-30', ?, true, false, " +
                    "NOW(), NOW(), 1, 9001)",
                projectId, projectId);
        jdbc.update("INSERT INTO task_predecessor(successor_id, predecessor_id, lag_days, dep_type, " +
                    "version) VALUES (9002, 9001, 0, 'FS', 1)");

        // Sanity: column exists before V121 runs.
        Integer colsBefore = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'tasks' AND column_name = 'depends_on_task_id'",
                Integer.class);
        assertThat(colsBefore).isEqualTo(1);

        // Apply V121 SQL body.
        jdbc.execute(loadV121Sql());

        // Column gone.
        Integer cols = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'tasks' AND column_name = 'depends_on_task_id'",
                Integer.class);
        assertThat(cols)
                .as("V121 must drop tasks.depends_on_task_id")
                .isZero();

        // task_predecessor row preserved.
        Integer predRows = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_predecessor " +
                "WHERE successor_id = 9002 AND predecessor_id = 9001",
                Integer.class);
        assertThat(predRows)
                .as("V121 must NOT touch task_predecessor")
                .isEqualTo(1);

        // Idempotency: running V121 again on a column that no longer exists
        // must be a no-op (DROP COLUMN IF EXISTS).
        jdbc.execute(loadV121Sql());
        Integer colsAfterRerun = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'tasks' AND column_name = 'depends_on_task_id'",
                Integer.class);
        assertThat(colsAfterRerun).isZero();

        // Cleanup: leave the schema in the same shape we found it. (DDL is
        // auto-committed and outlives any @Transactional, so leaking a column
        // back to other tests would be bad — but here we dropped it, which is
        // the post-V121 production state, which is what other tests expect.)
    }
}
