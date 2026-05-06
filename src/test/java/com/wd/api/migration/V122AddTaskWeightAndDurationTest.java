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
 * Verifies V122__add_task_weight_and_duration.sql adds two nullable INT
 * columns to {@code tasks} with no defaults. Loads the migration body
 * from the classpath and runs it against a Hibernate-built test schema.
 *
 * <p>Hibernate already creates the columns via the {@code @Column} mapping
 * on Task post-Task 2; this test bypasses that by dropping the columns
 * first, then runs V122, then asserts shape.
 */
class V122AddTaskWeightAndDurationTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void restoreColumns() {
        // Hibernate's create-drop will recreate next test, but make state
        // explicit for any test that runs in this same context.
        jdbc.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS weight INT");
        jdbc.execute("ALTER TABLE tasks ADD COLUMN IF NOT EXISTS duration_days INT");
    }

    private static String loadV122Sql() throws IOException {
        ClassPathResource resource =
                new ClassPathResource("db/migration/V122__add_task_weight_and_duration.sql");
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    @Test
    void v122_addsWeightAndDurationDays_nullable_noDefault() throws Exception {
        // Drop the columns Hibernate may have created so V122 actually runs the ADD.
        jdbc.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS weight");
        jdbc.execute("ALTER TABLE tasks DROP COLUMN IF EXISTS duration_days");

        jdbc.execute(loadV122Sql());

        // weight: INT, NULLABLE, no default
        var weightRow = jdbc.queryForMap(
                "SELECT data_type, is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'tasks' AND column_name = 'weight'");
        assertThat(weightRow.get("data_type")).isEqualTo("integer");
        assertThat(weightRow.get("is_nullable")).isEqualTo("YES");
        assertThat(weightRow.get("column_default")).isNull();

        // duration_days: INT, NULLABLE, no default
        var durationRow = jdbc.queryForMap(
                "SELECT data_type, is_nullable, column_default " +
                "FROM information_schema.columns " +
                "WHERE table_name = 'tasks' AND column_name = 'duration_days'");
        assertThat(durationRow.get("data_type")).isEqualTo("integer");
        assertThat(durationRow.get("is_nullable")).isEqualTo("YES");
        assertThat(durationRow.get("column_default")).isNull();
    }
}
