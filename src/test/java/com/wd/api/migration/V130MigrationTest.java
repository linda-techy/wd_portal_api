package com.wd.api.migration;

import com.wd.api.testsupport.FlywayMigrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class V130MigrationTest extends FlywayMigrationTestBase {

    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        jdbc.execute("DROP TABLE IF EXISTS idempotency_responses");
        jdbc.execute("ALTER TABLE site_reports DROP COLUMN IF EXISTS idempotency_key");
        jdbc.execute("ALTER TABLE delay_logs   DROP COLUMN IF EXISTS idempotency_key");
    }

    @AfterEach
    void cleanup() { prepare(); }

    private static String loadSql() throws IOException {
        return StreamUtils.copyToString(
                new ClassPathResource("db/migration/V130__add_idempotency_support.sql").getInputStream(),
                StandardCharsets.UTF_8);
    }

    @Test
    void siteReportsGainsIdempotencyKeyColumn() throws IOException {
        jdbc.execute(loadSql());
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'site_reports'",
                String.class);
        assertThat(cols).contains("idempotency_key");
    }

    @Test
    void delayLogsGainsIdempotencyKeyColumn() throws IOException {
        jdbc.execute(loadSql());
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'delay_logs'",
                String.class);
        assertThat(cols).contains("idempotency_key");
    }

    @Test
    void idempotencyResponsesTableHasExpectedColumns() throws IOException {
        jdbc.execute(loadSql());
        List<String> cols = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'idempotency_responses' ORDER BY column_name",
                String.class);
        assertThat(cols).contains(
                "idempotency_key", "request_method", "request_path",
                "response_status", "response_body", "response_content_type",
                "cached_at", "expires_at");
    }

    @Test
    void idempotencyResponsesUniqueOnPrimaryKey() throws IOException {
        jdbc.execute(loadSql());
        LocalDateTime exp = LocalDateTime.now().plusHours(24);
        jdbc.update("INSERT INTO idempotency_responses " +
                "(idempotency_key, request_method, request_path, response_status, response_body, response_content_type, cached_at, expires_at) " +
                "VALUES ('k1','POST','/api/site-reports',201,'{}','application/json',NOW(),?)", exp);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO idempotency_responses " +
                        "(idempotency_key, request_method, request_path, response_status, response_body, response_content_type, cached_at, expires_at) " +
                        "VALUES ('k1','POST','/api/site-reports',201,'{}','application/json',NOW(),?)", exp))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void idempotencyResponsesExpiresAtIndexExists() throws IOException {
        jdbc.execute(loadSql());
        Integer hits = jdbc.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'idempotency_responses' " +
                        "AND indexname = 'idx_idempotency_responses_expires_at'", Integer.class);
        assertThat(hits).isEqualTo(1);
    }
}
