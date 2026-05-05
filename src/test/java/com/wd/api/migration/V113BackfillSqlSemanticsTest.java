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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the semantics of V113__backfill_task_predecessor.sql.
 *
 * <p>Flyway is disabled in tests because the V1 baseline is
 * documentation-only — V1-era schema is bootstrapped by Hibernate from
 * {@code @Entity} definitions, then Flyway would normally run V2+ on top.
 * This test loads V113's SQL body from
 * {@code classpath:db/migration/V113__backfill_task_predecessor.sql} and
 * exercises its semantics directly against the Hibernate-built schema.
 *
 * <p>Reading the SQL from the classpath (rather than inlining a copy)
 * means future edits to V113 are exercised by this test automatically:
 * if a change breaks the documented semantics (idempotency,
 * malformed-row tolerance, etc.), the test fails immediately.
 */
@Transactional
class V113BackfillSqlSemanticsTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static String loadV113Sql() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("db/migration/V113__backfill_task_predecessor.sql");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private void seedFixture() {
        // Insert a tiny customer_projects row so FK constraints pass.
        jdbc.update("""
            INSERT INTO customer_projects (id, project_uuid, name, location, is_design_agreement_signed,
                                           created_at, updated_at, version)
            VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1)
            ON CONFLICT (id) DO NOTHING
            """, 9001L, UUID.randomUUID(), "test-fixture-project", "test-location");

        jdbc.update("DELETE FROM task_predecessor");
        jdbc.update("DELETE FROM tasks WHERE id IN (?,?,?,?,?,?)",
                1001L, 1002L, 1003L, 1004L, 1005L, 1006L);

        for (long id : new long[]{1001, 1002, 1003, 1004, 1005, 1006}) {
            jdbc.update("""
                INSERT INTO tasks (id, title, status, priority, due_date, project_id,
                                   customer_visible, monsoon_sensitive,
                                   created_at, updated_at, version)
                VALUES (?, ?, 'PENDING', 'MEDIUM', '2026-12-31', ?, true, false,
                        NOW(), NOW(), 1)
                """, id, "Task " + id, 9001L);
        }
        jdbc.update("UPDATE tasks SET depends_on_task_id = ? WHERE id = ?", 1001L, 1002L);
        jdbc.update("UPDATE tasks SET depends_on_task_id = ? WHERE id = ?", 1002L, 1003L);
        jdbc.update("UPDATE tasks SET depends_on_task_id = ? WHERE id = ?", 1002L, 1004L);
        jdbc.update("UPDATE tasks SET depends_on_task_id = ? WHERE id = ?", 1005L, 1005L); // self-loop
        jdbc.update("UPDATE tasks SET depends_on_task_id = ? WHERE id = ?", 99999L, 1006L); // dangling
    }

    @Test
    void backfill_copiesValidRows_skipsDanglingAndSelfLoops() throws IOException {
        seedFixture();

        // Run the actual V113 SQL body loaded from the classpath.
        jdbc.execute(loadV113Sql());

        Integer rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_predecessor WHERE successor_id IN (?,?,?,?,?,?)",
                Integer.class, 1001L, 1002L, 1003L, 1004L, 1005L, 1006L);
        assertThat(rowCount).isEqualTo(3);

        assertThat(jdbc.queryForObject(
                "SELECT predecessor_id FROM task_predecessor WHERE successor_id = ?",
                Long.class, 1002L)).isEqualTo(1001L);
        assertThat(jdbc.queryForObject(
                "SELECT predecessor_id FROM task_predecessor WHERE successor_id = ?",
                Long.class, 1003L)).isEqualTo(1002L);
        assertThat(jdbc.queryForObject(
                "SELECT predecessor_id FROM task_predecessor WHERE successor_id = ?",
                Long.class, 1004L)).isEqualTo(1002L);
    }

    @Test
    void backfill_isIdempotent_onSecondRun() throws IOException {
        seedFixture();

        String sql = loadV113Sql();

        jdbc.execute(sql);
        Integer firstRunCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_predecessor", Integer.class);

        // Re-run V113 — ON CONFLICT (successor_id, predecessor_id) DO NOTHING
        // should leave the table count unchanged.
        jdbc.execute(sql);
        Integer secondRunCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_predecessor", Integer.class);

        assertThat(secondRunCount).isEqualTo(firstRunCount);
    }

    @Test
    void backfill_skipsDanglingForeignKeyRows() throws IOException {
        // Targeted scenario: a single task with depends_on_task_id pointing
        // to a non-existent task. V113's INNER JOIN tasks p must drop the row.
        jdbc.update("""
            INSERT INTO customer_projects (id, project_uuid, name, location, is_design_agreement_signed,
                                           created_at, updated_at, version)
            VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1)
            ON CONFLICT (id) DO NOTHING
            """, 9002L, UUID.randomUUID(), "dangling-test-project", "test-location");

        jdbc.update("DELETE FROM task_predecessor WHERE successor_id IN (?,?)", 2001L, 2002L);
        jdbc.update("DELETE FROM tasks WHERE id IN (?,?)", 2001L, 2002L);

        jdbc.update("""
            INSERT INTO tasks (id, title, status, priority, due_date, project_id,
                               customer_visible, monsoon_sensitive,
                               created_at, updated_at, version)
            VALUES (?, ?, 'PENDING', 'MEDIUM', '2026-12-31', ?, true, false,
                    NOW(), NOW(), 1)
            """, 2001L, "Task 2001", 9002L);
        // Point at a task id that does not exist (no row 88_888 anywhere).
        jdbc.update("UPDATE tasks SET depends_on_task_id = ? WHERE id = ?", 88_888L, 2001L);

        jdbc.execute(loadV113Sql());

        Integer rowCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_predecessor WHERE successor_id = ?",
                Integer.class, 2001L);
        assertThat(rowCount)
                .as("dangling depends_on_task_id row must be skipped by V113's INNER JOIN")
                .isZero();
    }
}
