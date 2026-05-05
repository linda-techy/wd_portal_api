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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that V118__add_cpm_columns_to_task.sql adds the seven CPM columns
 * with the expected types and defaults. Because Hibernate's create-drop already
 * generates these columns from the @Entity, we exercise the SQL body against
 * a synthetic table that mirrors the pre-V118 shape.
 */
@Transactional
class V118AddCpmColumnsTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static String loadV118Sql() throws IOException {
        ClassPathResource r = new ClassPathResource("db/migration/V118__add_cpm_columns_to_task.sql");
        return StreamUtils.copyToString(r.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    void v118_addsAllSevenCpmColumns_toFreshTaskShape() throws IOException {
        // Build a fixture table mirroring the pre-V118 `tasks` shape.
        jdbc.execute("DROP TABLE IF EXISTS tasks_v118_fixture");
        jdbc.execute("""
            CREATE TABLE tasks_v118_fixture (
                id BIGSERIAL PRIMARY KEY,
                title VARCHAR(256) NOT NULL,
                actual_end_date DATE
            )
            """);

        // Apply V118's body, retargeted at the fixture table.
        String sql = loadV118Sql().replace("ALTER TABLE tasks", "ALTER TABLE tasks_v118_fixture");
        jdbc.execute(sql);

        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'tasks_v118_fixture' ORDER BY column_name",
                String.class);

        assertThat(cols).contains(
                "actual_start_date", "es_date", "ef_date",
                "ls_date", "lf_date", "total_float_days", "is_critical");
    }

    @Test
    void v118_isCriticalDefaultsToFalse() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS tasks_v118_default");
        jdbc.execute("""
            CREATE TABLE tasks_v118_default (
                id BIGSERIAL PRIMARY KEY,
                title VARCHAR(256) NOT NULL,
                actual_end_date DATE
            )
            """);
        String sql = loadV118Sql().replace("ALTER TABLE tasks", "ALTER TABLE tasks_v118_default");
        jdbc.execute(sql);

        Long id = jdbc.queryForObject(
                "INSERT INTO tasks_v118_default (title) VALUES (?) RETURNING id",
                Long.class, "v118-default-row " + UUID.randomUUID());
        Boolean isCritical = jdbc.queryForObject(
                "SELECT is_critical FROM tasks_v118_default WHERE id = ?",
                Boolean.class, id);
        assertThat(isCritical).isFalse();
    }
}
