package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies V128__create_change_request_tasks.sql semantics. Because the
 * codebase uses Hibernate {@code create-drop} for tests, the new tables are
 * already materialised by the JPA entities once they exist; this test
 * therefore validates the schema shape, the PostgreSQL-level constraints,
 * and the FK cascade behaviour against the live tables.
 */
@Transactional
class V128CreateChangeRequestTasksTest extends FlywayMigrationTestBase {

    @Autowired private JdbcTemplate jdbc;

    @Test
    void v128_createsChangeRequestTasksTable() {
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'change_request_tasks' ORDER BY column_name",
                String.class);
        assertThat(cols).contains(
                "id", "change_request_id", "sequence", "name", "role_hint",
                "duration_days", "weight_factor", "monsoon_sensitive",
                "is_payment_milestone", "floor_loop", "optional_cost",
                "created_at", "updated_at", "version");
    }

    @Test
    void v128_createsChangeRequestTaskPredecessorsTable() {
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'change_request_task_predecessors' " +
                "ORDER BY column_name",
                String.class);
        assertThat(cols).contains(
                "id", "successor_cr_task_id", "predecessor_cr_task_id",
                "lag_days", "dep_type", "created_at", "version");
    }

    @Test
    void v128_uniqueConstraintOnSuccessorPredecessorPair() {
        Long crId = insertCr();
        Long t1 = insertCrTask(crId, 1, "A");
        Long t2 = insertCrTask(crId, 2, "B");
        jdbc.update("INSERT INTO change_request_task_predecessors " +
                    "(successor_cr_task_id, predecessor_cr_task_id, lag_days, dep_type, " +
                    "created_at, updated_at, version) " +
                    "VALUES (?, ?, 0, 'FS', NOW(), NOW(), 1)", t2, t1);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO change_request_task_predecessors " +
                "(successor_cr_task_id, predecessor_cr_task_id, lag_days, dep_type, " +
                "created_at, updated_at, version) " +
                "VALUES (?, ?, 0, 'FS', NOW(), NOW(), 1)", t2, t1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v128_cascadesOnChangeRequestDelete() {
        Long crId = insertCr();
        insertCrTask(crId, 1, "A");
        insertCrTask(crId, 2, "B");
        Integer before = jdbc.queryForObject(
                "SELECT COUNT(*) FROM change_request_tasks WHERE change_request_id = ?",
                Integer.class, crId);
        assertThat(before).isEqualTo(2);

        jdbc.update("DELETE FROM project_variations WHERE id = ?", crId);

        Integer after = jdbc.queryForObject(
                "SELECT COUNT(*) FROM change_request_tasks WHERE change_request_id = ?",
                Integer.class, crId);
        assertThat(after).isZero();
    }

    @Test
    void v128_durationDaysCheckRejectsZero() {
        Long crId = insertCr();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO change_request_tasks " +
                "(change_request_id, sequence, name, duration_days, monsoon_sensitive, " +
                "is_payment_milestone, floor_loop, created_at, updated_at, version) " +
                "VALUES (?, 1, 'X', 0, false, false, 'NONE', NOW(), NOW(), 1)", crId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v128_floorLoopCheckRejectsUnknownValues() {
        Long crId = insertCr();
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO change_request_tasks " +
                "(change_request_id, sequence, name, duration_days, monsoon_sensitive, " +
                "is_payment_milestone, floor_loop, created_at, updated_at, version) " +
                "VALUES (?, 1, 'X', 1, false, false, 'WEEKLY', NOW(), NOW(), 1)", crId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long insertCr() {
        Long projectId = jdbc.queryForObject(
                "INSERT INTO customer_projects (project_uuid, name, location, " +
                "is_design_agreement_signed, created_at, updated_at, version) " +
                "VALUES (?, 'V128 fixture', 'Loc', false, NOW(), NOW(), 1) RETURNING id",
                Long.class, UUID.randomUUID());
        return jdbc.queryForObject(
                "INSERT INTO project_variations " +
                "(project_id, description, estimated_amount, status, client_approved, " +
                "created_at, updated_at, version) " +
                "VALUES (?, 'V128 fixture CR', ?, 'DRAFT', false, NOW(), NOW(), 1) RETURNING id",
                Long.class, projectId, new BigDecimal("0"));
    }

    private Long insertCrTask(Long crId, int seq, String name) {
        return jdbc.queryForObject(
                "INSERT INTO change_request_tasks " +
                "(change_request_id, sequence, name, duration_days, monsoon_sensitive, " +
                "is_payment_milestone, floor_loop, created_at, updated_at, version) " +
                "VALUES (?, ?, ?, 1, false, false, 'NONE', NOW(), NOW(), 1) RETURNING id",
                Long.class, crId, seq, name);
    }
}
