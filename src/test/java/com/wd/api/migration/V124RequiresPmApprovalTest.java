package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V124__add_requires_pm_approval.sql adds a single boolean column
 * to {@code project_schedule_config}. Loads the SQL body from the classpath
 * and runs it against a Hibernate-built test schema. Mirrors V122 pattern.
 */
class V124RequiresPmApprovalTest extends FlywayMigrationTestBase {

    @Autowired private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        // DDL outlives @Transactional rollback; reset schema for sibling tests.
        jdbc.execute("ALTER TABLE project_schedule_config DROP COLUMN IF EXISTS requires_pm_approval");
    }

    private static String loadSql() throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V124__add_requires_pm_approval.sql")
                        .getInputStream(),
                StandardCharsets.UTF_8);
    }

    @Test
    void v124_addsBooleanColumnNotNullDefaultFalse() throws IOException {
        // Drop the column Hibernate may have created so V124 actually runs the ADD.
        jdbc.execute("ALTER TABLE project_schedule_config DROP COLUMN IF EXISTS requires_pm_approval");

        jdbc.execute(loadSql());

        String dataType = jdbc.queryForObject(
                "SELECT data_type FROM information_schema.columns " +
                "WHERE table_name = 'project_schedule_config' AND column_name = 'requires_pm_approval'",
                String.class);
        assertThat(dataType).isEqualTo("boolean");

        String isNullable = jdbc.queryForObject(
                "SELECT is_nullable FROM information_schema.columns " +
                "WHERE table_name = 'project_schedule_config' AND column_name = 'requires_pm_approval'",
                String.class);
        assertThat(isNullable).isEqualTo("NO");

        String columnDefault = jdbc.queryForObject(
                "SELECT column_default FROM information_schema.columns " +
                "WHERE table_name = 'project_schedule_config' AND column_name = 'requires_pm_approval'",
                String.class);
        assertThat(columnDefault).contains("false");
    }

    @Test
    void v124_isIdempotent() throws IOException {
        String sql = loadSql();
        jdbc.execute(sql);
        jdbc.execute(sql);
    }
}
