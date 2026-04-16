package com.wd.api.testsupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class HarnessSmokeTest extends TestcontainersPostgresBase {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void flywayRanAndPortalRolesExists() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'portal_roles'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
