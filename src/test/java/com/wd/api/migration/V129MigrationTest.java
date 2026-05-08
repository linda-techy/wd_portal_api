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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies V129__create_otp_tokens.sql semantics. Mirrors the V127MigrationTest
 * pattern: load SQL from classpath, apply against Hibernate-built schema via
 * JdbcTemplate, then assert columns + indexes exist.
 */
class V129MigrationTest extends FlywayMigrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        jdbc.execute("DROP TABLE IF EXISTS otp_tokens");
    }

    private static String loadSql() throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V129__create_otp_tokens.sql")
                        .getInputStream(),
                StandardCharsets.UTF_8);
    }

    @Test
    void otpTokensTableHasExpectedColumns() throws IOException {
        jdbc.execute(loadSql());

        List<Map<String, Object>> cols = jdbc.queryForList(
            "SELECT column_name, data_type, is_nullable, column_default " +
            "FROM information_schema.columns WHERE table_name = 'otp_tokens'");

        List<String> names = cols.stream().map(c -> (String) c.get("column_name")).toList();
        assertThat(names).contains(
            "id", "code_hash", "target_type", "target_id",
            "customer_user_id", "expires_at", "used_at",
            "attempts", "max_attempts", "created_at");
    }

    @Test
    void partialIndexExistsForActiveLookups() throws IOException {
        jdbc.execute(loadSql());

        Integer hits = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes " +
            "WHERE tablename = 'otp_tokens' AND indexname = 'idx_otp_tokens_target_unused'",
            Integer.class);
        assertThat(hits).isEqualTo(1);
    }

    @Test
    void rateLimitIndexExistsForCountQueries() throws IOException {
        jdbc.execute(loadSql());

        Integer hits = jdbc.queryForObject(
            "SELECT COUNT(*) FROM pg_indexes " +
            "WHERE tablename = 'otp_tokens' AND indexname = 'idx_otp_tokens_customer_created_at'",
            Integer.class);
        assertThat(hits).isEqualTo(1);
    }
}
