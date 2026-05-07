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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that V126__add_last_alerted_handover_date.sql adds the
 * {@code last_alerted_handover_date} column to {@code project_schedule_config}
 * as a nullable DATE column. Mirrors V118AddCpmColumnsTest's fixture-table
 * pattern so the test exercises the SQL body without colliding with the
 * Hibernate-managed live schema.
 */
@Transactional
class V126AddLastAlertedHandoverDateTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static String loadV126Sql() throws IOException {
        ClassPathResource r = new ClassPathResource(
                "db/migration/V126__add_last_alerted_handover_date.sql");
        return StreamUtils.copyToString(r.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    void v126_addsNullableLastAlertedHandoverDateColumn() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS psc_v126_fixture");
        jdbc.execute("""
            CREATE TABLE psc_v126_fixture (
                id BIGSERIAL PRIMARY KEY,
                project_id BIGINT NOT NULL UNIQUE
            )
            """);
        String sql = loadV126Sql().replace(
                "ALTER TABLE project_schedule_config",
                "ALTER TABLE psc_v126_fixture");
        jdbc.execute(sql);

        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_name = 'psc_v126_fixture' " +
                "AND column_name = 'last_alerted_handover_date'",
                String.class);
        assertThat(cols).containsExactly("last_alerted_handover_date");

        Boolean isNullable = jdbc.queryForObject(
                "SELECT (is_nullable = 'YES') FROM information_schema.columns " +
                "WHERE table_name = 'psc_v126_fixture' " +
                "AND column_name = 'last_alerted_handover_date'",
                Boolean.class);
        assertThat(isNullable).isTrue();
    }
}
