package com.wd.api.controller.scheduling;

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
 * Verifies the semantics of V117__seed_s1_roles_and_permissions.sql.
 *
 * <p>Flyway is disabled in the test profile (see {@link FlywayMigrationTestBase}); we therefore
 * load the V117 SQL body directly from the classpath and execute it against the
 * Hibernate-built schema. This means changes to V117 are exercised by this test
 * automatically.
 *
 * <p>Important — the V5 baseline in this codebase seeds role codes
 * {@code ADMIN}, {@code PROJECT_MANAGER}, {@code SITE_ENGINEER}, {@code FINANCE_OFFICER},
 * {@code PROCUREMENT_OFFICER}, {@code INTERIOR_DESIGNER}, etc. — but does NOT seed
 * {@code SUPER_ADMIN} or {@code ARCHITECT}. The V117 grant statement uses an INNER JOIN on
 * {@code portal_roles} and therefore silently drops any cell of the matrix whose role code is
 * absent. We assert the actual reachable count rather than the aspirational matrix count.
 */
@Transactional
class PermissionSeedingMigrationTest extends FlywayMigrationTestBase {

    @Autowired
    private JdbcTemplate jdbc;

    private static String loadSql(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * The Hibernate-generated test schema does not declare the production unique
     * constraints that V5's "DO $$ BEGIN ... ALTER TABLE ... ADD CONSTRAINT ..."
     * blocks install (V5 itself is skipped in test because Flyway is disabled).
     * V117 uses ON CONFLICT (code) / (name) / (role_id, permission_id) which
     * Postgres rejects unless those constraints exist. Install them once per
     * test transaction so V117 can execute as it does in production.
     */
    /** V5 baseline role codes referenced by V117's matrix that we must seed in test. */
    private void seedV5BaselineRolesUsedByMatrix() {
        ensureUniqueConstraintsForOnConflict();
        jdbc.update("""
            INSERT INTO portal_roles (name, description, code)
            VALUES
                ('Administrator',         'baseline V5',  'ADMIN'),
                ('Project Manager',       'baseline V5',  'PROJECT_MANAGER'),
                ('Site Engineer',         'baseline V5',  'SITE_ENGINEER'),
                ('Procurement Officer',   'baseline V5',  'PROCUREMENT_OFFICER'),
                ('Finance Officer',       'baseline V5',  'FINANCE_OFFICER'),
                ('Interior Designer',     'baseline V5',  'INTERIOR_DESIGNER')
            ON CONFLICT (code) DO NOTHING
            """);
    }

     private void ensureUniqueConstraintsForOnConflict() {
        jdbc.execute("""
            DO $$ BEGIN
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'portal_roles_code_unique'
                ) THEN
                    BEGIN
                        ALTER TABLE portal_roles
                            ADD CONSTRAINT portal_roles_code_unique UNIQUE (code);
                    EXCEPTION WHEN others THEN NULL;
                    END;
                END IF;
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'portal_permissions_name_unique'
                ) THEN
                    BEGIN
                        ALTER TABLE portal_permissions
                            ADD CONSTRAINT portal_permissions_name_unique UNIQUE (name);
                    EXCEPTION WHEN others THEN NULL;
                    END;
                END IF;
                IF NOT EXISTS (
                    SELECT 1 FROM pg_constraint WHERE conname = 'portal_role_permissions_pkey_pair'
                ) THEN
                    BEGIN
                        ALTER TABLE portal_role_permissions
                            ADD CONSTRAINT portal_role_permissions_pkey_pair UNIQUE (role_id, permission_id);
                    EXCEPTION WHEN others THEN NULL;
                    END;
                END IF;
            END $$;
            """);
    }

    private static final List<String> NEW_PERMISSION_NAMES = List.of(
            "WBS_TEMPLATE_VIEW", "WBS_TEMPLATE_MANAGE", "PROJECT_WBS_CLONE",
            "HOLIDAY_VIEW", "HOLIDAY_MANAGE", "PROJECT_HOLIDAY_OVERRIDE",
            "PROJECT_SCHEDULE_CONFIG_EDIT", "MONSOON_WARNING_VIEW");

    private static final List<String> NEW_ROLE_CODES = List.of(
            "SCHEDULER", "QUANTITY_SURVEYOR", "MANAGEMENT");

    @Test
    void v117_seedsThreeNewRoleCodes() throws IOException {
        ensureUniqueConstraintsForOnConflict();
        jdbc.execute(loadSql("db/migration/V117__seed_s1_roles_and_permissions.sql"));

        Integer roleCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_roles WHERE code IN ('SCHEDULER','QUANTITY_SURVEYOR','MANAGEMENT')",
                Integer.class);
        assertThat(roleCount).isEqualTo(3);

        for (String code : NEW_ROLE_CODES) {
            String name = jdbc.queryForObject(
                    "SELECT name FROM portal_roles WHERE code = ?", String.class, code);
            assertThat(name).as("role name for %s", code).isNotBlank();
        }
    }

    @Test
    void v117_seedsEightNewPermissions() throws IOException {
        ensureUniqueConstraintsForOnConflict();
        jdbc.execute(loadSql("db/migration/V117__seed_s1_roles_and_permissions.sql"));

        Integer permCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_permissions WHERE name IN ("
                        + NEW_PERMISSION_NAMES.stream().map(n -> "'" + n + "'")
                                .reduce((a, b) -> a + "," + b).orElseThrow()
                        + ")",
                Integer.class);
        assertThat(permCount).isEqualTo(8);
    }

    @Test
    void v117_grantsMatchActualReachableMatrixRows() throws IOException {
        seedV5BaselineRolesUsedByMatrix();
        jdbc.execute(loadSql("db/migration/V117__seed_s1_roles_and_permissions.sql"));

        // Per V117 SQL header: SUPER_ADMIN and ARCHITECT are referenced but not seeded
        // in this codebase's V5 baseline; their matrix cells are silently dropped.
        // Reachable count per row (matrix cells minus missing roles):
        //   WBS_TEMPLATE_VIEW              = 11 - 2 = 9
        //   WBS_TEMPLATE_MANAGE            = 3  - 1 = 2
        //   PROJECT_WBS_CLONE              = 3  - 1 = 2
        //   HOLIDAY_VIEW                   = 11 - 2 = 9
        //   HOLIDAY_MANAGE                 = 2  - 1 = 1
        //   PROJECT_HOLIDAY_OVERRIDE       = 4  - 1 = 3
        //   PROJECT_SCHEDULE_CONFIG_EDIT   = 4  - 1 = 3
        //   MONSOON_WARNING_VIEW           = 6  - 1 = 5
        // Total                                    = 34

        Integer grantCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM portal_role_permissions rp
                JOIN portal_permissions p ON p.id = rp.permission_id
                WHERE p.name IN ('WBS_TEMPLATE_VIEW','WBS_TEMPLATE_MANAGE','PROJECT_WBS_CLONE',
                                 'HOLIDAY_VIEW','HOLIDAY_MANAGE','PROJECT_HOLIDAY_OVERRIDE',
                                 'PROJECT_SCHEDULE_CONFIG_EDIT','MONSOON_WARNING_VIEW')
                """, Integer.class);
        assertThat(grantCount).isEqualTo(34);
    }

    @Test
    void v117_isIdempotent() throws IOException {
        ensureUniqueConstraintsForOnConflict();
        String sql = loadSql("db/migration/V117__seed_s1_roles_and_permissions.sql");

        jdbc.execute(sql);
        Integer firstPermCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_permissions", Integer.class);
        Integer firstRoleCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_roles", Integer.class);
        Integer firstGrantCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_role_permissions", Integer.class);

        jdbc.execute(sql);
        Integer secondPermCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_permissions", Integer.class);
        Integer secondRoleCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_roles", Integer.class);
        Integer secondGrantCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM portal_role_permissions", Integer.class);

        assertThat(secondPermCount).isEqualTo(firstPermCount);
        assertThat(secondRoleCount).isEqualTo(firstRoleCount);
        assertThat(secondGrantCount).isEqualTo(firstGrantCount);
    }

    @Test
    void v117_schedulerGetsAllExpectedPermissions() throws IOException {
        seedV5BaselineRolesUsedByMatrix();
        jdbc.execute(loadSql("db/migration/V117__seed_s1_roles_and_permissions.sql"));

        // SCHEDULER row in matrix: VIEW + MANAGE + CLONE + HOLIDAY_VIEW + HOLIDAY_OVERRIDE +
        // SCHEDULE_CONFIG_EDIT + MONSOON_WARNING_VIEW = 7 grants. (HOLIDAY_MANAGE not granted.)
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM portal_role_permissions rp
                JOIN portal_roles r ON r.id = rp.role_id
                JOIN portal_permissions p ON p.id = rp.permission_id
                WHERE r.code = 'SCHEDULER'
                  AND p.name IN ('WBS_TEMPLATE_VIEW','WBS_TEMPLATE_MANAGE','PROJECT_WBS_CLONE',
                                 'HOLIDAY_VIEW','PROJECT_HOLIDAY_OVERRIDE',
                                 'PROJECT_SCHEDULE_CONFIG_EDIT','MONSOON_WARNING_VIEW')
                """, Integer.class);
        assertThat(count).isEqualTo(7);

        Integer holidayManage = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM portal_role_permissions rp
                JOIN portal_roles r ON r.id = rp.role_id
                JOIN portal_permissions p ON p.id = rp.permission_id
                WHERE r.code = 'SCHEDULER' AND p.name = 'HOLIDAY_MANAGE'
                """, Integer.class);
        assertThat(holidayManage).as("SCHEDULER must NOT have HOLIDAY_MANAGE per matrix").isZero();
    }

    @Test
    void v117_quantitySurveyorAndManagementGrants() throws IOException {
        seedV5BaselineRolesUsedByMatrix();
        jdbc.execute(loadSql("db/migration/V117__seed_s1_roles_and_permissions.sql"));

        // QUANTITY_SURVEYOR: WBS_TEMPLATE_VIEW + HOLIDAY_VIEW = 2
        Integer qsCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM portal_role_permissions rp
                JOIN portal_roles r ON r.id = rp.role_id
                JOIN portal_permissions p ON p.id = rp.permission_id
                WHERE r.code = 'QUANTITY_SURVEYOR'
                  AND p.name IN ('WBS_TEMPLATE_VIEW','HOLIDAY_VIEW')
                """, Integer.class);
        assertThat(qsCount).isEqualTo(2);

        // MANAGEMENT: WBS_TEMPLATE_VIEW + HOLIDAY_VIEW + MONSOON_WARNING_VIEW = 3
        Integer mgmtCount = jdbc.queryForObject("""
                SELECT COUNT(*) FROM portal_role_permissions rp
                JOIN portal_roles r ON r.id = rp.role_id
                JOIN portal_permissions p ON p.id = rp.permission_id
                WHERE r.code = 'MANAGEMENT'
                  AND p.name IN ('WBS_TEMPLATE_VIEW','HOLIDAY_VIEW','MONSOON_WARNING_VIEW')
                """, Integer.class);
        assertThat(mgmtCount).isEqualTo(3);
    }
}
