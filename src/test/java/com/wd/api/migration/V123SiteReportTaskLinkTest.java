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
 * Verifies V123__add_site_reports_task_id_and_type.sql semantics. Loads the
 * SQL body from the classpath and runs it against a Hibernate-built test
 * schema. Mirrors the V121/V122 pattern.
 *
 * <p>The Hibernate-bootstrapped schema already auto-creates {@code report_type}
 * (via {@link com.wd.api.model.SiteReport#reportType}); V123 uses
 * {@code IF NOT EXISTS} which makes the column-add a no-op there. We still
 * exercise the CHECK constraint and the partial index that the migration
 * unconditionally creates.
 */
class V123SiteReportTaskLinkTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        // DDL is auto-committed; tidy after test so other migration tests in
        // the same container start from a known state.
        jdbc.execute("DROP INDEX IF EXISTS idx_site_reports_task_completion");
        jdbc.execute("ALTER TABLE site_reports DROP CONSTRAINT IF EXISTS site_reports_report_type_chk");
        jdbc.execute("ALTER TABLE site_reports DROP COLUMN IF EXISTS task_id");
    }

    private static String loadSql() throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V123__add_site_reports_task_id_and_type.sql")
                        .getInputStream(),
                StandardCharsets.UTF_8);
    }

    private long insertProject(long id) {
        jdbc.update(
                "INSERT INTO customer_projects (id, project_uuid, name, location, " +
                "is_design_agreement_signed, created_at, updated_at, version) " +
                "VALUES (?, ?, ?, ?, false, NOW(), NOW(), 1) " +
                "ON CONFLICT (id) DO NOTHING",
                id, UUID.randomUUID(), "v123-test-" + id, "Test Location");
        return id;
    }

    @Test
    void v123_addsTaskIdColumnNullable() throws IOException {
        jdbc.execute(loadSql());
        String isNullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'site_reports' AND column_name = 'task_id'",
                String.class);
        assertThat(isNullable).isEqualTo("YES");
    }

    @Test
    void v123_reportTypeCheckRejectsUnknownValue() throws IOException {
        jdbc.execute(loadSql());
        long projectId = insertProject(91230L);
        // Insert a site report with bogus report_type → CHECK fails.
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO site_reports (project_id, title, report_type, " +
                "created_at, updated_at, version) VALUES (?, ?, ?, NOW(), NOW(), 1)",
                projectId, "t", "TOTALLY_INVALID"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v123_partialIndexExists() throws IOException {
        jdbc.execute(loadSql());
        Integer indexCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes " +
                "WHERE tablename = 'site_reports' AND indexname = 'idx_site_reports_task_completion'",
                Integer.class);
        assertThat(indexCount).isEqualTo(1);
    }

    @Test
    void v123_isIdempotent_secondRunIsNoOp() throws IOException {
        String sql = loadSql();
        jdbc.execute(sql);
        jdbc.execute(sql); // must not throw — IF NOT EXISTS / DROP IF EXISTS
    }
}
