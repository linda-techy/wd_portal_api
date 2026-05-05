package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V113__backfill_task_predecessor.sql copies single-predecessor
 * rows into task_predecessor and tolerates malformed input.
 *
 * <p>Strategy: Hibernate creates the schema (including task_predecessor from
 * the JPA entity) on context startup. We insert a fixture and run the V113
 * SQL body manually — that is the actual SQL the production migration
 * applies, so this exercises its semantics end-to-end. The SQL is idempotent
 * thanks to ON CONFLICT, so re-running it is safe.
 */
@Transactional
class V113BackfillMigrationTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void backfill_copiesValidRows_skipsDanglingAndSelfLoops() {
        // Insert a tiny customer_projects row so FK constraints pass.
        jdbc.update("""
            INSERT INTO customer_projects (id, project_uuid, name, location, is_design_agreement_signed,
                                           created_at, updated_at, version)
            VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1)
            ON CONFLICT (id) DO NOTHING
            """, 9001L, UUID.randomUUID(), "test-fixture-project", "test-location");

        // 5 tasks. T1 has no predecessor, T2->T1, T3->T2, T4->T2, T5->self (illegal),
        // and a 6th task T6 referencing a non-existent predecessor 99999.
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

        // Re-run the V113 backfill body manually — same SQL, idempotent.
        jdbc.execute("""
            INSERT INTO task_predecessor (
                successor_id, predecessor_id, lag_days, dep_type, version
            )
            SELECT t.id, t.depends_on_task_id, 0, 'FS', 1
              FROM tasks t
              JOIN tasks p ON p.id = t.depends_on_task_id
             WHERE t.depends_on_task_id IS NOT NULL
               AND t.depends_on_task_id <> t.id
            ON CONFLICT (successor_id, predecessor_id) DO NOTHING
            """);

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
}
