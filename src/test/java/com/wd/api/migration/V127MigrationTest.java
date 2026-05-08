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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies V127__cr_v2_state_machine_and_history.sql semantics. Mirrors
 * V125PendingPmApprovalStatusTest pattern: load SQL from classpath,
 * apply against Hibernate-built schema via JdbcTemplate.
 */
class V127MigrationTest extends FlywayMigrationTestBase {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.execute("ALTER TABLE project_variations DROP CONSTRAINT IF EXISTS project_variations_status_chk");
        jdbc.update(
            "DELETE FROM portal_role_permissions rp USING portal_permissions p " +
            "WHERE rp.permission_id = p.id AND p.name IN " +
            "('CR_SUBMIT','CR_COST','CR_SEND_TO_CUSTOMER','CR_SCHEDULE','CR_START','CR_COMPLETE','CR_REJECT')");
        jdbc.update(
            "DELETE FROM portal_permissions WHERE name IN " +
            "('CR_SUBMIT','CR_COST','CR_SEND_TO_CUSTOMER','CR_SCHEDULE','CR_START','CR_COMPLETE','CR_REJECT')");
    }

    private static String loadSql() throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V127__cr_v2_state_machine_and_history.sql")
                        .getInputStream(),
                StandardCharsets.UTF_8);
    }

    private void seedRoles() {
        jdbc.update("DELETE FROM portal_roles WHERE code IN " +
                "('ADMIN','PROJECT_MANAGER','SCHEDULER','QUANTITY_SURVEYOR','SITE_ENGINEER')");
        jdbc.update("INSERT INTO portal_roles(name, description, code) VALUES " +
                "('Admin','sys','ADMIN'), " +
                "('Project Manager','pm','PROJECT_MANAGER'), " +
                "('Scheduler','sched','SCHEDULER'), " +
                "('Quantity Surveyor','qs','QUANTITY_SURVEYOR'), " +
                "('Site Engineer','se','SITE_ENGINEER')");
    }

    private long insertProject(long id) {
        jdbc.update(
            "INSERT INTO customer_projects (id, project_uuid, name, location, " +
            "is_design_agreement_signed, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1) " +
            "ON CONFLICT (id) DO NOTHING",
            id, UUID.randomUUID(), "v127-test-" + id, "Loc");
        return id;
    }

    private long insertVariation(long projectId, String status) {
        return jdbc.queryForObject(
            "INSERT INTO project_variations (project_id, description, estimated_amount, status, " +
            "client_approved, created_at, updated_at, version) " +
            "VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1) RETURNING id",
            Long.class,
            projectId, "desc", new BigDecimal("1000"), status);
    }

    @Test
    void v127_dataFixupRenamesPendingApprovalRowsToCustomerApprovalPending() throws IOException {
        seedRoles();
        long projectId = insertProject(91270L);
        long crId = insertVariation(projectId, "PENDING_APPROVAL");

        jdbc.execute(loadSql());

        String status = jdbc.queryForObject(
            "SELECT status FROM project_variations WHERE id = ?", String.class, crId);
        assertThat(status).isEqualTo("CUSTOMER_APPROVAL_PENDING");
    }

    @Test
    void v127_checkConstraintAdmitsAllNineStates() throws IOException {
        seedRoles();
        jdbc.execute(loadSql());
        long projectId = insertProject(91271L);

        for (String s : new String[]{
                "DRAFT","SUBMITTED","COSTED","CUSTOMER_APPROVAL_PENDING",
                "APPROVED","SCHEDULED","IN_PROGRESS","COMPLETE","REJECTED"}) {
            insertVariation(projectId, s);
        }
    }

    @Test
    void v127_checkConstraintRejectsLegacyPendingApprovalLiteral() throws IOException {
        seedRoles();
        jdbc.execute(loadSql());
        long projectId = insertProject(91272L);

        assertThatThrownBy(() -> insertVariation(projectId, "PENDING_APPROVAL"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v127_seedsSevenPermissions() throws IOException {
        seedRoles();
        jdbc.execute(loadSql());
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM portal_permissions WHERE name IN " +
            "('CR_SUBMIT','CR_COST','CR_SEND_TO_CUSTOMER','CR_SCHEDULE','CR_START','CR_COMPLETE','CR_REJECT')",
            Integer.class);
        assertThat(count).isEqualTo(7);
    }

    @Test
    void v127_grantsExpectedRolePermissionPairs() throws IOException {
        seedRoles();
        jdbc.execute(loadSql());

        // Role-grant matrix from V127 (13 pairs total — see SQL):
        //   PROJECT_MANAGER:    CR_SUBMIT, CR_SEND_TO_CUSTOMER, CR_REJECT (3)
        //   SCHEDULER:          CR_SUBMIT, CR_SCHEDULE                  (2)
        //   QUANTITY_SURVEYOR:  CR_COST                                 (1)
        //   SITE_ENGINEER:      CR_START, CR_COMPLETE                   (2)
        //   ADMIN:              CR_SUBMIT, CR_COST, CR_SEND_TO_CUSTOMER, CR_SCHEDULE, CR_REJECT (5)
        //   = 13 grants
        Integer grantCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM portal_role_permissions rp " +
            "JOIN portal_roles r ON r.id = rp.role_id " +
            "JOIN portal_permissions p ON p.id = rp.permission_id " +
            "WHERE p.name IN ('CR_SUBMIT','CR_COST','CR_SEND_TO_CUSTOMER','CR_SCHEDULE','CR_START','CR_COMPLETE','CR_REJECT')",
            Integer.class);
        assertThat(grantCount)
            .as("V127 must grant exactly 13 (role, permission) pairs from the matrix")
            .isEqualTo(13);
    }

    @Test
    void v127_addsHistoryAuditColumns() throws IOException {
        seedRoles();
        jdbc.execute(loadSql());
        // change_request_approval_history is created by V127 (via CREATE TABLE IF NOT EXISTS).
        for (String col : new String[]{"from_status","to_status","otp_hash","customer_ip",
                "actor_user_id","customer_user_id","reason"}) {
            Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = 'change_request_approval_history' AND column_name = ?",
                Integer.class, col);
            assertThat(c).as("Column %s must exist on change_request_approval_history", col).isEqualTo(1);
        }
    }

    @Test
    void v127_isIdempotent() throws IOException {
        seedRoles();
        String sql = loadSql();
        jdbc.execute(sql);
        jdbc.execute(sql);  // Second apply must not throw.
    }
}
